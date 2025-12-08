package com.profitsoft.application.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.profitsoft.application.entities.Book;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BookJsonParser Tests")
public class BookJsonParserTest {

    @Test
    void parseArray_shouldCountTwo() throws Exception {
        String json = "[{\"title\":\"A\",\"author\":\"X\",\"year_published\":2000,\"genre\":\"Romance,Tragedy\"}," +
                "{\"title\":\"B\",\"author\":\"Y\",\"year_published\":2010,\"genre\":\"Romance\"}]";
        Path tmp = Files.createTempFile("books", ".json");
        Files.writeString(tmp, json);
        BookJsonParser p = new BookJsonParser();
        AtomicInteger cnt = new AtomicInteger();
        p.parseFile(tmp, (Book b) -> cnt.incrementAndGet());
        assertEquals(2, cnt.get());
    }
}