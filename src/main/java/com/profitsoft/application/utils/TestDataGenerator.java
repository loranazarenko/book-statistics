package com.profitsoft.application.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility class to generate test data for performance testing.
 * Usage: java -cp book-statistics.jar com.profitsoft.application.utils.TestDataGenerator <outputDir> <fileCount> <booksPerFile>
 */
@Slf4j
public class TestDataGenerator {

    private static final String[] AUTHORS = {
            "George Orwell", "Jane Austen", "William Shakespeare", "Charles Dickens",
            "Mark Twain", "Leo Tolstoy", "F. Scott Fitzgerald", "Ernest Hemingway",
            "Virginia Woolf", "James Joyce", "Franz Kafka", "Gabriel García Márquez",
            "Toni Morrison", "Haruki Murakami", "Margaret Atwood", "Salman Rushdie"
    };

    private static final String[] GENRES = {
            "Fiction", "Non-Fiction", "Science Fiction", "Fantasy", "Mystery",
            "Thriller", "Romance", "Horror", "Biography", "History",
            "Self-Help", "Poetry", "Drama", "Adventure", "Comedy",
            "Dystopian", "Political Fiction", "Satire", "Tragedy", "Epic"
    };

    private static final String[] TITLE_PREFIXES = {
            "The Great", "A Tale of", "The Secret", "Journey to", "The Last",
            "Beyond the", "In Search of", "The Lost", "Return to", "The Mystery of",
            "Adventures in", "The Chronicles of", "Echoes of", "Shadows of", "The Legend of"
    };

    private static final String[] TITLE_SUFFIXES = {
            "Dreams", "Tomorrow", "Yesterday", "Paradise", "Darkness",
            "Light", "Time", "Space", "Memory", "Hope",
            "Freedom", "Truth", "Justice", "Power", "Destiny"
    };

    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        if (args.length < 3) {
            log.error("Usage: java TestDataGenerator <outputDir> <fileCount> <booksPerFile>");
            log.error("Example: java TestDataGenerator ./test-data 10 1000");
            System.exit(1);
        }

        String outputDir = args[0];
        int fileCount = Integer.parseInt(args[1]);
        int booksPerFile = Integer.parseInt(args[2]);

        try {
            generateTestData(outputDir, fileCount, booksPerFile);
            log.info("Test data generation completed successfully!");
        } catch (IOException e) {
            log.error("Failed to generate test data: {}", e.getMessage());
            System.exit(1);
        }
    }

    public static void generateTestData(String outputDir, int fileCount, int booksPerFile) throws IOException {
        Path dirPath = Paths.get(outputDir);
        Files.createDirectories(dirPath);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        log.info("Generating test data...");
        log.info("Output directory: {}", dirPath.toAbsolutePath());
        log.info("Files to generate: {}", fileCount);
        log.info("Books per file: {}", booksPerFile);
        log.info("Total books: {}", fileCount * booksPerFile);

        for (int i = 0; i < fileCount; i++) {
            List<Map<String, Object>> books = new ArrayList<>();

            for (int j = 0; j < booksPerFile; j++) {
                Map<String, Object> book = new HashMap<>();
                book.put("title", generateTitle());
                book.put("author", AUTHORS[RANDOM.nextInt(AUTHORS.length)]);
                book.put("year_published", 1900 + RANDOM.nextInt(125)); // 1900-2024
                book.put("genre", generateGenres());
                books.add(book);
            }

            File outputFile = dirPath.resolve(String.format("books_%03d.json", i + 1)).toFile();
            mapper.writeValue(outputFile, books);

            if ((i + 1) % 10 == 0 || i == fileCount - 1) {
                log.info("Generated {} / {} files", i + 1, fileCount);
            }
        }

        log.info("Test data generated at: {}", dirPath.toAbsolutePath());
    }

    private static String generateTitle() {
        String prefix = TITLE_PREFIXES[RANDOM.nextInt(TITLE_PREFIXES.length)];
        String suffix = TITLE_SUFFIXES[RANDOM.nextInt(TITLE_SUFFIXES.length)];
        return prefix + " " + suffix;
    }

    private static String generateGenres() {
        int genreCount = 1 + RANDOM.nextInt(3); // 1-3 genres
        Set<String> selectedGenres = new HashSet<>();

        while (selectedGenres.size() < genreCount) {
            selectedGenres.add(GENRES[RANDOM.nextInt(GENRES.length)]);
        }

        return String.join(", ", selectedGenres);
    }
}