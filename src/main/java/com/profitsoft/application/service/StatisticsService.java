package com.profitsoft.application.service;

import com.profitsoft.application.entities.Book;
import com.profitsoft.application.entities.StatisticsItem;
import com.profitsoft.application.utils.BookJsonParser;
import com.profitsoft.application.utils.XmlStatisticsWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Service responsible for scanning a directory of JSON files, parsing them (possibly in parallel),
 * aggregating statistics for a given attribute and writing the result to an XML file.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class StatisticsService {

    private BookJsonParser parser = new BookJsonParser();

    /**
     * Process all JSON files in `directory` and compute statistics by `attribute`.
     *
     * @param directory directory with json files
     * @param attribute attribute name (e.g. "author","title","year_published","genre")
     * @param threads   number of threads to use
     * @return StatisticsResult containing metadata and output file
     * @throws Exception on fatal errors
     */
    public StatisticsResult processDirectory(File directory, String attribute, int threads) throws Exception {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(attribute, "attribute");
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        List<Path> files;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory.toPath(), "*.json")) {
            files = new ArrayList<>();
            for (Path p : ds) files.add(p);
        }

        if (files.isEmpty()) {
            log.warn("No JSON files found in directory: {}", directory);
            File out = createOutputFile(attribute);
            return new StatisticsResult(0, 0L, Collections.emptyList(), 0L, 0L, 0L, 0L, out); // 0L,
        }

        ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> representatives = new ConcurrentHashMap<>();
        AtomicLong bookCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        AttributeStrategy strategy = getStrategy(attribute.toLowerCase());

        int maxThreads = Math.max(1, Math.min(threads, Runtime.getRuntime().availableProcessors() * 2));

        long parsingStart = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        List<Future<?>> futures = new ArrayList<>(files.size());
        try {
            for (Path f : files) {
                futures.add(executor.submit(() -> {
                    try {
                        parser.parseFile(f, (Book book) -> {
                            try {
                                strategy.process(book, counts, representatives);
                                bookCount.incrementAndGet();
                            } catch (Exception e) {
                                log.error("Error processing book from {}: {}", f, e.getMessage(), e);
                                errorCount.incrementAndGet();
                            }
                        });
                    } catch (IOException e) {
                        log.error("Failed to parse file {}: {}", f, e.getMessage(), e);
                        errorCount.incrementAndGet();
                    } catch (RuntimeException e) {
                        log.error("Runtime error while parsing file {}: {}", f, e.getMessage(), e);
                        errorCount.incrementAndGet();
                    }
                }));
            }

            executor.shutdown();

            for (Future<?> future : futures) {
                try {
                     future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String causeMsg = cause != null ? cause.getMessage() : "unknown cause";
                    log.error("Execution error in task: {}", causeMsg, e);
                    errorCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrupted while waiting for parsing tasks: {}", e.getMessage(), e);
                    throw e;
                }
            }

            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                log.warn("Timed out waiting for parsing tasks; attempting shutdownNow");
                executor.shutdownNow();
                if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                    log.warn("Executor did not terminate after shutdownNow");
                }
            }
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
        long parsingEnd = System.currentTimeMillis();
        long parsingTimeMs = parsingEnd - parsingStart;
        List<StatisticsItem> statistics = counts.entrySet().stream()
                .map(e -> {
                    String key = e.getKey();
                    String display = toTitleCase(representatives.getOrDefault(key, key));
                    return new StatisticsItem(display, e.getValue().longValue());
                })
                .sorted(Comparator.comparingLong(StatisticsItem::getCount).reversed()
                        .thenComparing(StatisticsItem::getValue, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        long xmlStart = System.currentTimeMillis();
        File out = createOutputFile(attribute);
        new XmlStatisticsWriter().writeStatistics(out.toPath(), statistics);
        long xmlEnd = System.currentTimeMillis();
        long xmlTimeMs = xmlEnd - xmlStart;
        long totalTime = parsingTimeMs + xmlTimeMs;

        if (errorCount.get() > 0) {
            log.warn("Processed with {} errors", errorCount.get());
        }

        return new StatisticsResult(files.size(), bookCount.get(), statistics,
                parsingTimeMs, xmlTimeMs, totalTime, errorCount.get(), out);  //, statsTimeMs
    }

    private File createOutputFile(String attribute) {
        String safe = attribute.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
        return Paths.get("").toAbsolutePath().resolve("statistics_by_" + safe + ".xml").toFile();
    }

    private interface AttributeStrategy {
        void process(Book book, ConcurrentHashMap<String, LongAdder> counts,
                     ConcurrentHashMap<String, String> representatives);
    }

    private AttributeStrategy getStrategy(String attribute) {
        return switch (attribute) {
            case "genre" -> (book, counts, representatives) -> {
                List<String> genres = book.getGenres();
                if (genres != null) {
                    for (String g : genres) {
                        processValue(g, counts, representatives);
                    }
                }
            };
            case "author" ->
                    (book, counts, representatives) -> processValue(book.getAuthorName(), counts, representatives);
            case "title" -> (book, counts, representatives) -> processValue(book.getTitle(), counts, representatives);
            case "year_published" -> (book, counts, representatives) -> {
                Integer y = book.getYearPublished();
                if (y != null) {
                    String key = String.valueOf(y);
                    counts.computeIfAbsent(key, k -> new LongAdder()).increment();
                    representatives.putIfAbsent(key, key);
                }
            };
            default -> throw new IllegalArgumentException("Unsupported attribute: " + attribute);
        };
    }

    private void processValue(String raw, ConcurrentHashMap<String, LongAdder> counts,
                              ConcurrentHashMap<String, String> representatives) {
        if (raw == null || raw.trim().isEmpty()) return;
        String normalized = raw.trim().toLowerCase();
        representatives.putIfAbsent(normalized, raw.trim());
        counts.computeIfAbsent(normalized, k -> new LongAdder()).increment();
    }

    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        return Arrays.stream(input.toLowerCase().split("\\s+"))
                .map(word -> word.isEmpty() ? word : Character.toTitleCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    public record StatisticsResult(
            int fileCount,
            long bookCount,
            List<StatisticsItem> statistics,
            long parsingTimeMs,
            long xmlTimeMs,
            long totalTimeMs,
            long errorCount,
            File outputFile
    ) {
    }
}