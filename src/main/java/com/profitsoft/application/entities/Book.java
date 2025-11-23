package com.profitsoft.application.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Book entity representing a single book with its attributes.
 * Primary entity in Book-Author relationship (many-to-one).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Setter
    @JsonProperty("title")
    private String title;

    @JsonProperty("author")
    private Author author;

    @JsonProperty("year_published")
    private Integer yearPublished;

    @JsonProperty("genre")
    private List<String> genres = new ArrayList<>();

    /**
     * Set author from either String or Author object for backward compatibility
     */
    @JsonSetter("author")
    public void setAuthor(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Author cannot be null");
        } else if (value instanceof String s) {
            if (s.trim().isEmpty()) {
                throw new IllegalArgumentException("Author name cannot be empty");
            }
            this.author = new Author(s);
        } else if (value instanceof Author a) {
            this.author = a;
        } else {
            String str = value.toString().trim();
            if (str.isEmpty()) {
                throw new IllegalArgumentException("Author name cannot be empty");
            }
            this.author = new Author(str);
        }
    }

    @JsonSetter("year_published")
    public void setYearPublished(Integer year) {
        if (year != null && year <= 0) {
            throw new IllegalArgumentException("Year published must be positive");
        }
        this.yearPublished = year;
    }

    @JsonSetter("genre")
    public void setGenre(Object value) {
        this.genres = parseGenres(value);
    }

    private List<String> parseGenres(Object value) {
        if (value == null) return new ArrayList<>();

        if (value instanceof String s) {
            return Arrays.stream(s.split("\\s*([,;/])\\s*"))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
        }

        return List.of(value.toString().trim());
    }

    public List<String> getGenres() {
        return Collections.unmodifiableList(genres);
    }

    /**
     * Get author name as string for statistics
     */
    public String getAuthorName() {
        return author != null ? author.getName() : null;
    }
}