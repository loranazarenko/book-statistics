package com.profitsoft.application.utils;

import com.profitsoft.application.entities.Book;
import com.profitsoft.application.entities.StatisticsItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * Calculator for book statistics by various attributes.
 * Notes:
 * - Grouping is case-insensitive but display values are Title Case for stable expected output.
 * - Genres are counted as complete values (not split into words).
 * - Sorting: by count desc, then by display name case-insensitive (deterministic).
 */
public class StatisticsCalculator {
    private static final Set<String> SUPPORTED_ATTRIBUTES = Set.of(
            "title", "author", "year_published", "genre"
    );

    public List<StatisticsItem> calculate(List<Book> books, String attribute) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }

        if (attribute == null || attribute.isBlank()) {
            throw new IllegalArgumentException("Attribute cannot be null or empty");
        }

        String normalizedAttr = attribute.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ATTRIBUTES.contains(normalizedAttr)) {
            throw new IllegalArgumentException("Unsupported attribute: " + attribute +
                    ". Supported attributes are: " + String.join(", ", SUPPORTED_ATTRIBUTES));
        }

        return switch (normalizedAttr) {
            case "title" -> calculateByStringField(books, Book::getTitle);
            case "author" -> calculateByStringField(books, Book::getAuthorName);
            case "year_published" -> calculateByYear(books);
            case "genre" -> calculateByGenre(books);
            default -> throw new IllegalArgumentException("Unsupported attribute: " + attribute);
        };
    }

    public List<String> getSupportedAttributes() {
        return new ArrayList<>(SUPPORTED_ATTRIBUTES);
    }

    // Generic string field (title/author) handling: case-insensitive grouping,
    // preserve first-seen value but display normalized Title Case
    private List<StatisticsItem> calculateByStringField(List<Book> books, Function<Book, String> extractor) {
        if (books == null || books.isEmpty()) return Collections.emptyList();

        ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> representative = new ConcurrentHashMap<>();

        books.parallelStream().forEach(book -> {
            if (book == null) return;
            String raw = extractor.apply(book);
            if (raw == null) return;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) return;

            String normalized = trimmed.toLowerCase(Locale.ROOT);
            representative.putIfAbsent(normalized, trimmed);
            counts.computeIfAbsent(normalized, k -> new LongAdder()).increment();
        });

        return sortByCountWithTitleCase(counts, representative);
    }

    private List<StatisticsItem> calculateByYear(List<Book> books) {
        if (books == null || books.isEmpty()) return Collections.emptyList();

        ConcurrentHashMap<Integer, LongAdder> counts = new ConcurrentHashMap<>();

        books.parallelStream().forEach(book -> {
            if (book == null || book.getYearPublished() == null) return;
            counts.computeIfAbsent(book.getYearPublished(), k -> new LongAdder()).increment();
        });

        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Integer, LongAdder>>comparingLong(e -> e.getValue().longValue()).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(e -> new StatisticsItem(e.getKey().toString(), e.getValue().longValue()))
                .collect(Collectors.toList());
    }

    private List<StatisticsItem> calculateByGenre(List<Book> books) {
        if (books == null || books.isEmpty()) return Collections.emptyList();

        ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> representative = new ConcurrentHashMap<>();

        books.parallelStream().forEach(book -> {
            if (book == null) return;
            book.getGenres().forEach(genre -> {
                if (genre == null || genre.trim().isEmpty()) return;
                String normalized = genre.trim().toLowerCase(Locale.ROOT);
                representative.putIfAbsent(normalized, genre);
                counts.computeIfAbsent(normalized, k -> new LongAdder()).increment();
            });
        });

        return sortByCountWithTitleCase(counts, representative);
    }

    // Sort by count desc, then by key case-insensitive
    private List<StatisticsItem> sortByCountWithTitleCase(ConcurrentHashMap<String, LongAdder> counts, ConcurrentHashMap<String, String> representative) {
        if (counts == null || counts.isEmpty()) return Collections.emptyList();

        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, LongAdder>>comparingLong(e -> e.getValue().longValue())
                        .reversed()
                        .thenComparing(e -> {
                            String repr = representative.get(e.getKey());
                            String disp = (repr == null ? e.getKey() : repr).toLowerCase(Locale.ROOT);
                            return disp;
                        }))
                .map(e -> {
                    String repr = representative.get(e.getKey());
                    String displayRaw = repr == null ? e.getKey() : repr;
                    String display = toTitleCase(displayRaw);
                    return new StatisticsItem(display, e.getValue().longValue());
                })
                .collect(Collectors.toList());
    }

    // utility: "political fiction" -> "Political Fiction"
    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;

        return Arrays.stream(input.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .map(word -> {
                    if (word.isEmpty()) return word;
                    if (word.contains("-")) {
                        return Arrays.stream(word.split("-"))
                                .map(part -> part.isEmpty() ? "" :
                                        Character.toUpperCase(part.charAt(0)) + part.substring(1))
                                .collect(Collectors.joining("-"));
                    }
                    return Character.toUpperCase(word.charAt(0)) + word.substring(1);
                })
                .collect(Collectors.joining(" "));
    }
}
