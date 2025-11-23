package com.profitsoft.application;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.profitsoft.application.entities.StatisticsItem;
import com.profitsoft.application.utils.BookJsonParser;
import com.profitsoft.application.service.StatisticsService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StatisticsService Tests")
public class StatisticsServiceTest {

    @Test
    void serviceCountsGenres() throws Exception {
        Path dir = Files.createTempDirectory("booksdir");
        String json = "[{\"title\":\"A\",\"author\":\"X\",\"year_published\":2000,\"genre\":\"Romance,Tragedy\"}," +
                "{\"title\":\"B\",\"author\":\"Y\",\"year_published\":2010,\"genre\":\"Romance\"}]";
        Files.writeString(dir.resolve("a.json"), json);
        StatisticsService service = new StatisticsService();
        service.setParser(new BookJsonParser());
        var res = service.processDirectory(dir.toFile(), "genre", 2);
        assertFalse(res.statistics().isEmpty(), "Statistics list should not be empty");
        List<StatisticsItem> romanceItems = res.statistics().stream()
                .filter(item -> "Romance".equals(item.getValue()))
                .toList();

        assertFalse(romanceItems.isEmpty(), "Romance should be found in statistics");
        assertEquals(2L, res.statistics().getFirst().getCount());
        assertEquals(0L, res.errorCount()); // No errors
    }

    @Test
    void serviceHandlesErrors() throws Exception {
        Path dir = Files.createTempDirectory("booksdir");
        Files.writeString(dir.resolve("invalid.json"), "invalid json"); // Broken file
        StatisticsService service = new StatisticsService();
        service.setParser(new BookJsonParser());
        var res = service.processDirectory(dir.toFile(), "genre", 2);
        assertEquals(1L, res.errorCount()); // One error
        assertThat(res.statistics()).isEmpty();
    }
}