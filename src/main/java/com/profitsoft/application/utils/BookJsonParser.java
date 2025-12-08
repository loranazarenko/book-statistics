package com.profitsoft.application.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profitsoft.application.dto.BookPojo;
import com.profitsoft.application.entities.Book;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
@AllArgsConstructor
@NoArgsConstructor
public class BookJsonParser {

    private ObjectMapper mapper = new ObjectMapper();
    private JsonFactory factory = mapper.getFactory();

    public void parseFileAsPojo(Path file, Consumer<BookPojo> consumer)
            throws IOException {
        try (InputStream in = Files.newInputStream(file);
             JsonParser jp = factory.createParser(in)) {

            JsonToken token = jp.nextToken();
            if (token == null) {
                log.warn("Empty JSON file: {}", file);
                return;
            }

            if (token == JsonToken.START_ARRAY) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    if (jp.currentToken() == JsonToken.START_OBJECT) {
                        BookPojo book = mapper.readValue(jp, BookPojo.class);
                        consumer.accept(book);
                    } else {
                        jp.skipChildren();
                    }
                }
            } else if (token == JsonToken.START_OBJECT) {
                BookPojo book = mapper.readValue(jp, BookPojo.class);
                consumer.accept(book);
            } else {
                log.warn("Unsupported root token in {}: {}", file, token);
            }
        } catch (JsonParseException e) {
            log.error("Invalid JSON in {}: {}", file, e.getMessage(), e);
            throw new IOException("JSON parsing failed", e);
        } catch (IOException e) {
            log.error("IO error parsing {}: {}", file, e.getMessage(), e);
            throw e;
        }
    }

    public void parseFile(Path file, Consumer<Book> consumer) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             JsonParser jp = factory.createParser(in)) {

            JsonToken token = jp.nextToken();
            if (token == null) {
                log.warn("Empty JSON file: {}", file);
                return;
            }

            if (token == JsonToken.START_ARRAY) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    if (jp.currentToken() == JsonToken.START_OBJECT) {
                        Book book = mapper.readValue(jp, Book.class);
                        consumer.accept(book);
                    } else {
                        jp.skipChildren();
                    }
                }
            } else if (token == JsonToken.START_OBJECT) {
                Book book = mapper.readValue(jp, Book.class);
                consumer.accept(book);
            } else {
                log.warn("Unsupported root token in {}: {}", file, token);
            }
        } catch (JsonParseException e) {
            log.error("Invalid JSON in {}: {}", file, e.getMessage(), e);
            throw new IOException("JSON parsing failed", e);
        } catch (IOException e) {
            log.error("IO error parsing {}: {}", file, e.getMessage(), e);
            throw e;
        }
    }
}