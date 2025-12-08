package com.profitsoft.application.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.*;
import java.util.stream.Collectors;

import lombok.*;

/**
 * Book entity representing a single book with its attributes.
 * Primary entity in Book-Author relationship (many-to-one).
 */
@Getter
@Entity
@Table(name = "book")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(exclude = "id")
@ToString(exclude = "author")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @JsonProperty("title")
    @Column(nullable = false, length = 500)
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @JsonProperty("author")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @JsonProperty("year_published")
    @Column(name = "year_published")
    @Min(value = 1, message = "Year published must be at least 1")
    @Max(2025)
    private Integer yearPublished;

    @JsonProperty("genre")
    @ElementCollection
    @Builder.Default
    @CollectionTable(name = "book_genres",
            joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "genre", length = 500)
    private List<String> genres = new ArrayList<>();

    @JsonSetter("author")
    public void setAuthor(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Author cannot be null");
        } else if (value instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Author name cannot be empty");
            }
            this.author = new Author(s);
        } else if (value instanceof Author a) {
            this.author = a;
        } else if (value instanceof Map<?, ?> map) {
            Object nameObj = map.get("name");
            String name = (nameObj != null) ? nameObj.toString().trim() : "";
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Author 'name' is missing or empty in JSON object");
            }
            String country = map.get("country") instanceof String c ? c.trim() : null;
            Integer birthYear = null;
            Object byObj = map.get("birth_year");
            if (byObj != null) {
                try {
                    birthYear = Integer.valueOf(byObj.toString().trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid birth_year format", e);
                }
            }
            this.author = new Author(name, country, birthYear);
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

    @JsonSetter("yearPublished")
    public void setYearPublishedCamelCase(Integer year) {
        setYearPublished(year);
    }

    @JsonSetter("genre")
    public void setGenres(Object value) {
        this.genres = parseGenres(value);
    }

    private List<String> parseGenres(Object value) {
        if (value == null) return new ArrayList<>();
        if (value instanceof String s) {
            return Arrays.stream(s.split("\\s*[,;/]\\s*"))
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
