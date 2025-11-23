package com.profitsoft.application;

import static org.assertj.core.api.Assertions.*;

import com.profitsoft.application.entities.Author;
import com.profitsoft.application.entities.Book;
import com.profitsoft.application.entities.StatisticsItem;
import com.profitsoft.application.utils.StatisticsCalculator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StatisticsCalculator Tests")
class StatisticsCalculatorTest {

    private StatisticsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new StatisticsCalculator();
    }

    @Test
    @DisplayName("Should calculate statistics by author with Author object")
    void testCalculateByAuthorObject() {
        List<Book> books = createTestBooksWithAuthorObjects();

        List<StatisticsItem> stats = calculator.calculate(books, "author");

        assertThat(stats).isNotEmpty();
        assertThat(stats).isSortedAccordingTo((a, b) -> Long.compare(b.getCount(), a.getCount()));
    }

    @Test
    @DisplayName("Should calculate statistics by genre without splitting words")
    void testCalculateByGenreNoSplit() {
        List<Book> books = createTestBooks();

        List<StatisticsItem> stats = calculator.calculate(books, "genre");

        assertThat(stats).isNotEmpty();

        // "Political Fiction" should be counted as ONE genre, not split into "Political" and "Fiction"
        assertThat(stats.stream().map(StatisticsItem::getValue))
                .contains("Political Fiction")
                .doesNotContain("Political", "Fiction");

        StatisticsItem politicalFiction = stats.stream()
                .filter(s -> s.getValue().equals("Political Fiction"))
                .findFirst()
                .orElseThrow();
        assertThat(politicalFiction.getCount()).isEqualTo(1);

        // Romance should appear in 2 books
        StatisticsItem romance = stats.stream()
                .filter(s -> s.getValue().equals("Romance"))
                .findFirst()
                .orElseThrow();
        assertThat(romance.getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle backward compatibility with string author")
    void testBackwardCompatibilityAuthor() {
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author"); // String input
        book.setYearPublished(2024);

        assertThat(book.getAuthor()).isNotNull();
        assertThat(book.getAuthorName()).isEqualTo("Test Author");
    }

    @Test
    @DisplayName("Should handle Author object")
    void testAuthorObject() {
        Book book = new Book();
        book.setTitle("Test Book");
        Author author = new Author("Test Author", "USA", 1970);
        book.setAuthor(author);
        book.setYearPublished(2024);

        assertThat(book.getAuthor()).isNotNull();
        assertThat(book.getAuthor().getName()).isEqualTo("Test Author");
        assertThat(book.getAuthor().getCountry()).isEqualTo("USA");
        assertThat(book.getAuthor().getBirthYear()).isEqualTo(1970);
    }

    @Test
    @DisplayName("Should return empty for null or empty books")
    void testEmptyBooks() {
        assertThat(calculator.calculate(null, "genre")).isEmpty();
        assertThat(calculator.calculate(new ArrayList<>(), "author")).isEmpty();
    }

    @Test
    @DisplayName("Should sort by count desc then value asc on equal counts")
    void testSortingEqualCounts() {
        List<Book> books = new ArrayList<>();
        Book b1 = new Book();
        b1.setGenre(List.of("A", "B"));
        Book b2 = new Book();
        b2.setGenre(List.of("C", "B"));
        books.add(b1);
        books.add(b2);

        List<StatisticsItem> stats = calculator.calculate(books, "genre");

        assertThat(stats.get(0).getValue()).isEqualTo("B"); // count 2
        assertThat(stats.get(1).getValue()).isEqualTo("A"); // count 1, before C
        assertThat(stats.get(2).getValue()).isEqualTo("C");
    }

    @Test
    @DisplayName("Should ignore null years")
    void testIgnoreNullYear() {
        List<Book> books = new ArrayList<>();
        Book b = new Book();
        b.setYearPublished(null);
        books.add(b);

        List<StatisticsItem> stats = calculator.calculate(books, "year_published");
        assertThat(stats).isEmpty();
    }

    private List<Book> createTestBooks() {
        List<Book> books = new ArrayList<>();

        Book book1 = new Book();
        book1.setTitle("1984");
        book1.setAuthor("George Orwell");
        book1.setYearPublished(1949);
        book1.setGenre(List.of("Dystopian", "Political Fiction"));

        Book book2 = new Book();
        book2.setTitle("Pride and Prejudice");
        book2.setAuthor("Jane Austen");
        book2.setYearPublished(1813);
        book2.setGenre(List.of("Romance", "Satire"));

        Book book3 = new Book();
        book3.setTitle("Romeo and Juliet");
        book3.setAuthor("William Shakespeare");
        book3.setYearPublished(1597);
        book3.setGenre(List.of("Romance", "Tragedy"));

        books.add(book1);
        books.add(book2);
        books.add(book3);

        return books;
    }

    private List<Book> createTestBooksWithAuthorObjects() {
        List<Book> books = new ArrayList<>();

        Book book1 = new Book();
        book1.setTitle("1984");
        book1.setAuthor(new Author("George Orwell", "UK", 1903));
        book1.setYearPublished(1949);
        book1.setGenre(List.of("Dystopian", "Political Fiction"));

        Book book2 = new Book();
        book2.setTitle("Animal Farm");
        book2.setAuthor(new Author("George Orwell", "UK", 1903));
        book2.setYearPublished(1945);
        book2.setGenre(List.of("Satire", "Political Fiction"));

        books.add(book1);
        books.add(book2);

        return books;
    }
}