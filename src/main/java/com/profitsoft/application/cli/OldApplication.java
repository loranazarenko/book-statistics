package com.profitsoft.application.cli;


import com.profitsoft.application.service.StatisticsService;
import com.profitsoft.application.utils.BookJsonParser;
import com.profitsoft.application.utils.ResultPrinter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Old main application entry point.
 * Provides console interface for statistics generation.
 */
@Slf4j
public class OldApplication {

    private static final String USAGE = """
            OPTIONS:
              --dir <path>       Path to directory with JSON files (required)
              --attribute <name> Attribute: title, author, year_published, genre (required)
              --threads <count>  Number of threads (optional, default: 4)
            Usage: java -jar book-statistics.jar --dir <path> --attribute <name> [--threads <count>]
            Supported attributes: title, author, year_published, genre
            Example:
              java -jar book-statistics.jar --dir ./books --attribute genre --threads 4
            """;

    private static final List<String> SUPPORTED = List.of("title", "author", "year_published", "genre");

    public static void main(String[] args) {
        ApplicationConfig cfg = parseArguments(args);
        if (cfg == null) {
            log.info(USAGE);
            System.exit(1);
        }

        Path dirPath = Paths.get(cfg.directory()).toAbsolutePath();
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            log.error("Directory does not exist: {}", dirPath);
            System.exit(1);
        }

        try {
            BookJsonParser parser = new BookJsonParser();
            StatisticsService service = new StatisticsService(parser);
            long startTime = System.currentTimeMillis();
            var result = service.processDirectory(dirPath.toFile(), cfg.attribute(), cfg.threadCount());
            long endTime = System.currentTimeMillis();
            ResultPrinter.print(result);
            log.info("Total execution time: {} ms", endTime - startTime);
        } catch (Exception e) {
            log.error("Error occurred during processing: {}", e.getMessage());
            System.exit(2);
        }
    }

    private static ApplicationConfig parseArguments(String[] args) {
        if (args == null || args.length == 0) {
            return interactivePrompt();
        }
        String dir = null;
        String attr = null;
        int threads = 4;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dir" -> {
                    if (i + 1 < args.length) dir = args[++i];
                    else {
                        log.error("--dir requires value");
                        return null;
                    }
                }
                case "--attribute" -> {
                    if (i + 1 < args.length) attr = args[++i];
                    else {
                        log.error("--attribute requires value");
                        return null;
                    }
                }
                case "--threads" -> {
                    if (i + 1 < args.length) {
                        try {
                            threads = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException ex) {
                            log.error("Invalid threads");
                            return null;
                        }
                    } else {
                        log.error("--threads requires value");
                        return null;
                    }
                }
                default -> {
                    log.error("Unknown arg: {}", args[i]);
                    return null;
                }
            }
        }

        if (dir == null || attr == null) {
            log.error("Missing --dir or --attribute");
            return null;
        }
        String aNorm = attr.toLowerCase();
        if (!SUPPORTED.contains(aNorm)) {
            log.error("Unsupported attribute: {}", attr);
            return null;
        }
        if (threads < 1) {
            log.error("threads must be >=1");
            return null;
        }

        int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
        if (threads > maxThreads) {
            log.warn("threads capped to {}", maxThreads);
            threads = maxThreads;
        }

        return new ApplicationConfig(dir, aNorm, threads);
    }

    private static ApplicationConfig interactivePrompt() {
        try (Scanner sc = new Scanner(System.in)) {
            log.info("Directory path: ");
            String dir = sc.nextLine().trim();
            log.info("Attribute (title/author/year_published/genre): ");
            String attr = sc.nextLine().trim();
            log.info("Threads (default 4): ");
            String th = sc.nextLine().trim();
            int threads = 4;
            if (!th.isBlank()) threads = Integer.parseInt(th);
            return new ApplicationConfig(dir, attr.toLowerCase(), threads);
        } catch (Exception e) {
            log.error("Input error: {}", e.getMessage());
            return null;
        }
    }
}

record ApplicationConfig(String directory, String attribute, int threadCount) {
}

