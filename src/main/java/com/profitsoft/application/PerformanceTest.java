package com.profitsoft.application;

import com.profitsoft.application.service.StatisticsService;
import com.profitsoft.application.utils.BookJsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Performance testing script to compare execution times with different thread counts.
 * Usage: java -cp book-statistics.jar com.profitsoft.application.PerformanceTest <directory> <attribute>
 */
@Slf4j
public class PerformanceTest {

    private static final int[] THREAD_COUNTS = {1, 2, 4, 8};
    private static final int WARMUP_RUNS = 2;
    private static final int TEST_RUNS = 5;

    public static void main(String[] args) {
        if (args.length < 2) {
            log.error("Usage: java PerformanceTest <directory> <attribute>");
            System.exit(1);
        }

        String directory = args[0];
        String attribute = args[1];

        Path dirPath = Paths.get(directory).toAbsolutePath();
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            log.error("Directory does not exist: {}", dirPath);
            System.exit(1);
        }

        log.info("=".repeat(80));
        log.info("PERFORMANCE TEST");
        log.info("Directory: {}", dirPath);
        log.info("Attribute: {}", attribute);
        log.info("Warmup runs: {}", WARMUP_RUNS);
        log.info("Test runs per configuration: {}", TEST_RUNS);
        log.info("=".repeat(80));

        List<PerformanceResult> results = new ArrayList<>();

        for (int threadCount : THREAD_COUNTS) {
            log.info("\n{}", "-".repeat(80));
            log.info("Testing with {} thread(s)", threadCount);
            log.info("-".repeat(80));

            log.info("Warming up...");
            for (int i = 0; i < WARMUP_RUNS; i++) {
                runTest(dirPath.toFile(), attribute, threadCount, false);
            }

            log.info("Running {} test iterations...", TEST_RUNS);
            List<Long> times = new ArrayList<>();
            List<Long> parsingTimes = new ArrayList<>();
            List<Long> xmlTimes = new ArrayList<>();

            for (int i = 0; i < TEST_RUNS; i++) {
                var result = runTest(dirPath.toFile(), attribute, threadCount, true);
                times.add(result.totalTimeMs());
                parsingTimes.add(result.parsingTimeMs());
                xmlTimes.add(result.xmlTimeMs());
            }

            long avgTotal = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
            long avgParsing = (long) parsingTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long avgXml = (long) xmlTimes.stream().mapToLong(Long::longValue).average().orElse(0);

            long minTotal = times.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxTotal = times.stream().mapToLong(Long::longValue).max().orElse(0);

            results.add(new PerformanceResult(
                    threadCount, avgTotal, minTotal, maxTotal,
                    avgParsing, avgXml));

            log.info("Average total time: {} ms", avgTotal);
            log.info("Min: {} ms, Max: {} ms", minTotal, maxTotal);
            log.info("Breakdown - Parsing: {} ms, XML: {} ms",
                    avgParsing, avgXml);
        }

        log.info("\n{}", "=".repeat(80));
        log.info("SUMMARY");
        log.info("=".repeat(80));
        log.info(String.format("%-10s | %-12s | %-12s | %-12s | %-10s",
                "Threads", "Avg Time(ms)", "Min(ms)", "Max(ms)", "Speedup"));
        log.info("-".repeat(80));

        long baselineTime = results.getFirst().avgTime;
        for (PerformanceResult result : results) {
            double speedup = (double) baselineTime / result.avgTime;
            log.info(String.format("%-10d | %-12d | %-12d | %-12d | %.2fx",
                    result.threads, result.avgTime, result.minTime,
                    result.maxTime, speedup));
        }

        log.info("=".repeat(80));
        log.info("\nDetailed breakdown:");
        log.info(String.format("%-10s | %-12s | %-12s",
                "Threads", "Parsing(ms)", "XML(ms)"));
        log.info("-".repeat(80));
        for (PerformanceResult result : results) {
            log.info(String.format("%-10d | %-12d | %-12d",
                    result.threads, result.avgParsing, result.avgXml));
        }
        log.info("=".repeat(80));

        log.info("\nRECOMMENDATIONS:");
        PerformanceResult best = results.stream()
                .min(Comparator.comparingLong(a -> a.avgTime))
                .orElse(results.getFirst());
        log.info("Best performance: {} threads with {} ms average time",
                best.threads, best.avgTime);

        double efficiency = (double) baselineTime / (best.threads * best.avgTime);
        log.info("Parallel efficiency: {}%", Math.round(efficiency * 100));
    }

    private static StatisticsService.StatisticsResult runTest(
            File directory, String attribute, int threads, boolean logDetails) {
        try {
            BookJsonParser parser = new BookJsonParser();
            StatisticsService service = new StatisticsService(parser);

            long startTime = System.currentTimeMillis();
            var result = service.processDirectory(directory, attribute, threads);
            long endTime = System.currentTimeMillis();

            if (logDetails) {
                log.info("  Run completed: {} ms (Files: {}, Books: {})",
                        endTime - startTime, result.fileCount(), result.bookCount());
            }

            return result;
        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private record PerformanceResult(
            int threads,
            long avgTime,
            long minTime,
            long maxTime,
            long avgParsing,
            long avgXml
    ) {}
}