package com.profitsoft.application.utils;

import com.profitsoft.application.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultPrinter {
    public static void print(StatisticsService.StatisticsResult result) {
        log.info("=== Results ===");
        log.info("Files processed: {}", result.fileCount());
        log.info("Total books parsed: {}", result.bookCount());
        log.info("Unique values: {}", result.statistics().size());
        log.info("");
        log.info("=== Top 10 ===");
        result.statistics().stream()
                .limit(10)
                .forEach(it -> log.info("  {}: {}", it.getValue(), it.getCount()));
        log.info("Output: {}", result.outputFile().getAbsolutePath());
    }
}