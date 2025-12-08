package com.profitsoft.application.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import com.profitsoft.application.entities.StatisticsItem;
import com.profitsoft.application.service.StatisticsService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("StatisticsService Integration Tests")
class StatisticsServiceTest {

    @TempDir
    Path tempDir;

    private final StatisticsService service = new StatisticsService(new BookJsonParser());

    // region Genre tests
    @Test
    @DisplayName("Should calculate genre statistics correctly: 'Political Fiction' as one genre, 'Romance' appears twice")
    void testGenreStatisticsWithCompositeAndRepeatedGenres() throws Exception {
        // Given
        String jsonContent = """
            [
              {
                "title": "1984",
                "author": "George Orwell",
                "year_published": 1949,
                "genre": "Dystopian, Political Fiction"
              },
              {
                "title": "Pride and Prejudice",
                "author": "Jane Austen",
                "year_published": 1813,
                "genre": "Romance, Satire"
              },
              {
                "title": "Romeo and Juliet",
                "author": "William Shakespeare",
                "year_published": 1597,
                "genre": "Romance, Tragedy"
              }
            ]
            """;
        Path jsonFile = tempDir.resolve("books.json");
        Files.writeString(jsonFile, jsonContent);

        // When
        var result = service.processDirectory(tempDir.toFile(), "genre", 2);

        // Then
        assertThat(result.errorCount()).isEqualTo(0);
        assertThat(result.bookCount()).isEqualTo(3);
        assertThat(result.fileCount()).isEqualTo(1);
        assertThat(result.statistics()).isNotEmpty();

        List<StatisticsItem> stats = result.statistics();

        // Check top item
        StatisticsItem first = stats.getFirst();
        assertThat(first.getValue()).isEqualTo("Romance");
        assertThat(first.getCount()).isEqualTo(2L);

        // Check "Political Fiction" is preserved as single value (not split)
        assertThat(stats.stream().map(StatisticsItem::getValue))
                .contains("Political Fiction")
                .doesNotContain("Political", "Fiction");

        StatisticsItem politicalFiction = stats.stream()
                .filter(s -> "Political Fiction".equals(s.getValue()))
                .findFirst()
                .orElseThrow();
        assertThat(politicalFiction.getCount()).isEqualTo(1L);

        // Check sorting: by count DESC, then value ASC (case-insensitive)
        // Expected order: Romance(2), Dystopian(1), Political Fiction(1), Satire(1), Tragedy(1)
        // Among count=1: lex order → Dystopian, Political Fiction, Satire, Tragedy
        assertThat(stats)
                .extracting(StatisticsItem::getValue, StatisticsItem::getCount)
                .containsExactly(
                        tuple("Romance", 2L),
                        tuple("Dystopian", 1L),
                        // "Political Fiction" < "Satire" lexicographically (P < S)
                        tuple("Political Fiction", 1L),
                        tuple("Satire", 1L),
                        tuple("Tragedy", 1L)
                );
    }

    @Test
    @DisplayName("Should handle empty or whitespace-only genres gracefully")
    void testHandleEmptyGenres() throws Exception {
        String jsonContent = """
            [
              {
                "title": "Book A",
                "author": "Author A",
                "genre": "  ,,  Fiction  ,, "
              },
              {
                "title": "Book B",
                "author": "Author B",
                "genre": ""
              }
            ]
            """;
        Files.writeString(tempDir.resolve("books.json"), jsonContent);

        var result = service.processDirectory(tempDir.toFile(), "genre", 1);

        assertThat(result.statistics()).hasSize(1);
        StatisticsItem item = result.statistics().getFirst();
        assertThat(item.getValue()).isEqualTo("Fiction");
        assertThat(item.getCount()).isEqualTo(1L);
    }
    // endregion

    // region Author tests
    @Test
    @DisplayName("Should calculate statistics by author (string and object formats)")
    void testAuthorStatistics() throws Exception {
        String jsonContent = """
            [
              {
                "title": "1984",
                "author": "George Orwell",
                "year_published": 1949,
                "genre": "Dystopian"
              },
              {
                "title": "Animal Farm",
                "author": {
                  "name": "George Orwell",
                  "country": "UK",
                  "birth_year": 1903
                },
                "year_published": 1945,
                "genre": "Satire"
              },
              {
                "title": "Pride and Prejudice",
                "author": "Jane Austen",
                "year_published": 1813,
                "genre": "Romance"
              }
            ]
            """;
        Files.writeString(tempDir.resolve("books.json"), jsonContent);

        var result = service.processDirectory(tempDir.toFile(), "author", 2);

        assertThat(result.statistics()).hasSize(2);
        assertThat(result.statistics().getFirst().getValue()).isEqualTo("George Orwell");
        assertThat(result.statistics().getFirst().getCount()).isEqualTo(2L);
        assertThat(result.statistics().get(1).getValue()).isEqualTo("Jane Austen");
    }
    // endregion

    // region Error handling
    @Test
    @DisplayName("Should continue processing despite invalid JSON files and report errors")
    void testHandlesInvalidJsonFiles() throws Exception {
        // Valid file
        Files.writeString(tempDir.resolve("valid.json"), """
            [{"title":"A","author":"X","genre":"Fiction"}]
            """);

        // Invalid JSON
        Files.writeString(tempDir.resolve("invalid.json"), "invalid json { abc ]");

        // Corrupted but starts with array
        Files.writeString(tempDir.resolve("broken.json"), "[{]");

        var result = service.processDirectory(tempDir.toFile(), "genre", 2);

        assertThat(result.errorCount()).isGreaterThanOrEqualTo(2); // at least 2 failed
        assertThat(result.bookCount()).isEqualTo(1); // only 1 book parsed
        assertThat(result.statistics()).hasSize(1);
        assertThat(result.statistics().getFirst().getValue()).isEqualTo("Fiction");
    }
    // endregion

    // region Edge cases
    @Test
    @DisplayName("Should return empty statistics for empty directory")
    void testEmptyDirectory() throws Exception {
        var result = service.processDirectory(tempDir.toFile(), "genre", 1);
        assertThat(result.statistics()).isEmpty();
        assertThat(result.bookCount()).isEqualTo(0);
        assertThat(result.fileCount()).isEqualTo(0);
        assertThat(result.errorCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should ignore null years in year_published statistics")
    void testIgnoreNullYear() throws Exception {
        String jsonContent = """
            [
              { "title": "Book A", "author": "A", "year_published": null },
              { "title": "Book B", "author": "B", "year_published": 2000 },
              { "title": "Book C", "author": "C" }
            ]
            """;
        Files.writeString(tempDir.resolve("books.json"), jsonContent);

        var result = service.processDirectory(tempDir.toFile(), "year_published", 1);

        assertThat(result.statistics()).hasSize(1);
        StatisticsItem item = result.statistics().getFirst();
        assertThat(item.getValue()).isEqualTo("2000");
        assertThat(item.getCount()).isEqualTo(1L);
    }
    // endregion
}