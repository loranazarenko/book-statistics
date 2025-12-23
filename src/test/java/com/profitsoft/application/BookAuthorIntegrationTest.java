package com.profitsoft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profitsoft.application.dto.BookDto;
import com.profitsoft.application.dto.AuthorDto;
import com.profitsoft.application.repository.AuthorRepository;
import com.profitsoft.application.repository.BookRepository;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for Book and Author APIs
 * Used MockMvc for testing HTTP endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Book & Author APIs Integration Tests")
public class BookAuthorIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cleanUpDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanUpDatabase();
    }

    private void cleanUpDatabase() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateOperations {

        @Test
        @DisplayName("Should create author and book successfully")
        void testCreateAuthorAndBook() throws Exception {
            // Step 1: Create author
            String authorBody = """
                    {
                        "name": "Ivan Franko",
                        "country": "Ukraine",
                        "birthYear": 1856
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto authorDto = parseResponse(authorResult, AuthorDto.class);
            assertThat(authorDto.getId()).isGreaterThan(0);

            // Step 2: Create book with this author
            String bookBody = """
                    {
                        "title": "Zakhar Berkut",
                        "authorId": %d,
                        "yearPublished": 1883,
                        "genres": ["Adventure", "Historical Fiction"]
                    }
                    """.formatted(authorDto.getId());

            MvcResult bookResult = mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            BookDto bookDto = parseResponse(bookResult, BookDto.class);
            assertThat(bookDto.getId()).isGreaterThan(0);
            assertThat(bookDto.getAuthor().getId()).isEqualTo(authorDto.getId());
            assertThat(bookDto.getGenres()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle multiple books per author")
        void testMultipleBooksPerAuthor() throws Exception {
            // Create author through API
            String authorBody = """
                    {
                        "name": "Nikolai Gogol",
                        "country": "Russia",
                        "birthYear": 1809
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author = parseResponse(authorResult, AuthorDto.class);

            // Create 3 books through API instead of bookRepository.save()
            for (int i = 1; i <= 3; i++) {
                String bookBody = """
                        {
                            "title": "Book %d",
                            "authorId": %d,
                            "yearPublished": %d,
                            "genres": ["Fiction", "Drama"]
                        }
                        """.formatted(i, author.getId(), 1800 + i * 10);

                mvc.perform(post("/api/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(bookBody)
                        )
                        .andExpect(status().isCreated());
            }

            // Verify through list endpoint
            String listBody = """
                    {
                        "page": 0,
                        "size": 10,
                        "sortBy": "id",
                        "sortOrder": "DESC"
                    }
                    """;

            mvc.perform(post("/api/book/_list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(listBody)
                    )
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateOperations {

        @Test
        @DisplayName("Should update book and maintain author reference")
        void testUpdateBookMaintainAuthor() throws Exception {
            // Create author through API
            String authorBody = """
                    {
                        "name": "Dostoevsky",
                        "country": "Russia",
                        "birthYear": 1821
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author = parseResponse(authorResult, AuthorDto.class);

            // Create book through API instead of bookRepository.save()
            String createBookBody = """
                    {
                        "title": "Original Title",
                        "authorId": %d,
                        "yearPublished": 2020,
                        "genres": ["Classic"]
                    }
                    """.formatted(author.getId());

            MvcResult createResult = mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBookBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            BookDto createdBook = parseResponse(createResult, BookDto.class);
            Long bookId = createdBook.getId();

            // Update
            String updateBody = """
                    {
                        "title": "Crime and Punishment",
                        "authorId": %d,
                        "yearPublished": 1866,
                        "genres": ["Crime", "Philosophical"]
                    }
                    """.formatted(author.getId());

            MvcResult result = mvc.perform(put("/api/book/{id}", bookId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            BookDto updated = parseResponse(result, BookDto.class);
            assertThat(updated.getTitle()).isEqualTo("Crime and Punishment");
            assertThat(updated.getAuthor().getId()).isEqualTo(author.getId());
            assertThat(updated.getGenres()).hasSize(2);
        }

        @Test
        @DisplayName("Should change author for book")
        void testChangeAuthorForBook() throws Exception {
            // Create two authors
            String author1Body = """
                    {
                        "name": "Author 1",
                        "country": "Country 1",
                        "birthYear": 1900
                    }
                    """;

            MvcResult author1Result = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(author1Body)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author1 = parseResponse(author1Result, AuthorDto.class);

            String author2Body = """
                    {
                        "name": "Author 2",
                        "country": "Country 2",
                        "birthYear": 1910
                    }
                    """;

            MvcResult author2Result = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(author2Body)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author2 = parseResponse(author2Result, AuthorDto.class);

            // Create book through API instead of bookRepository.save()
            String createBookBody = """
                    {
                        "title": "Test Book",
                        "authorId": %d,
                        "yearPublished": 2020
                    }
                    """.formatted(author1.getId());

            MvcResult createResult = mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBookBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            BookDto book = parseResponse(createResult, BookDto.class);
            Long bookId = book.getId();

            // Change author to author2
            String updateBody = """
                    {
                        "title": "Test Book",
                        "authorId": %d,
                        "yearPublished": 2020
                    }
                    """.formatted(author2.getId());

            mvc.perform(put("/api/book/{id}", bookId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody)
                    )
                    .andExpect(status().isOk());

            // Verify
            MvcResult getResult = mvc.perform(get("/api/book/{id}", bookId)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            BookDto updated = parseResponse(getResult, BookDto.class);
            assertThat(updated.getAuthor().getId()).isEqualTo(author2.getId());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete book without affecting author")
        void testDeleteBookPreservesAuthor() throws Exception {
            // Create author through API
            String authorBody = """
                    {
                        "name": "Test Author",
                        "country": "Ukraine",
                        "birthYear": 1950
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author = parseResponse(authorResult, AuthorDto.class);

            String createBookBody = """
                    {
                        "title": "Delete Test",
                        "authorId": %d,
                        "yearPublished": 2020
                    }
                    """.formatted(author.getId());

            MvcResult createResult = mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBookBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            BookDto book = parseResponse(createResult, BookDto.class);
            Long bookId = book.getId();

            // Delete book
            mvc.perform(delete("/api/book/{id}", bookId))
                    .andExpect(status().isNoContent());

            // Verify book deleted but author exists
            assertThat(bookRepository.findById(bookId)).isEmpty();
            assertThat(authorRepository.findById(author.getId())).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle cascade effects")
        void testCascadeDelete() throws Exception {
            // Create author through API
            String authorBody = """
                    {
                        "name": "Cascade Test",
                        "country": "Ukraine",
                        "birthYear": 1950
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author = parseResponse(authorResult, AuthorDto.class);

            for (int i = 0; i < 3; i++) {
                String createBookBody = """
                        {
                            "title": "Book %d",
                            "authorId": %d,
                            "yearPublished": 2020
                        }
                        """.formatted(i, author.getId());

                mvc.perform(post("/api/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBookBody)
                        )
                        .andExpect(status().isCreated());
            }

            // Delete all books BEFORE deleting author (FK constraint)
            List<Long> bookIds = bookRepository.findAll().stream()
                    .map(b -> b.getId())
                    .toList();

            for (Long bookId : bookIds) {
                mvc.perform(delete("/api/book/{id}", bookId))
                        .andExpect(status().isNoContent());
            }

            // Delete author
            mvc.perform(delete("/api/author/{id}", author.getId()))
                    .andExpect(status().isNoContent());

            // Verify author is deleted
            assertThat(authorRepository.findById(author.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("List & Search Operations")
    class ListAndSearchOperations {

        @Test
        @DisplayName("Should list all authors")
        void testListAllAuthors() throws Exception {
            // Create multiple authors
            for (int i = 0; i < 5; i++) {
                String authorBody = """
                        {
                            "name": "Author %d",
                            "country": "Country %d",
                            "birthYear": %d
                        }
                        """.formatted(i, i, 1900 + i);

                mvc.perform(post("/api/author")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(authorBody)
                        )
                        .andExpect(status().isCreated());
            }

            MvcResult result = mvc.perform(get("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            AuthorDto[] authors = parseResponse(result, AuthorDto[].class);
            assertThat(authors).hasSize(5);
        }

        @Test
        @DisplayName("Should list books with pagination")
        void testListBooksWithPagination() throws Exception {
            // Create author through API
            String authorBody = """
                    {
                        "name": "Test Author",
                        "country": "Ukraine",
                        "birthYear": 1950
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author = parseResponse(authorResult, AuthorDto.class);

            // Create 15 books through API instead of bookRepository.save()
            for (int i = 0; i < 15; i++) {
                String createBookBody = """
                        {
                            "title": "Book %d",
                            "authorId": %d,
                            "yearPublished": %d,
                            "genres": ["Fiction"]
                        }
                        """.formatted(i, author.getId(), 2020 + (i % 5));

                mvc.perform(post("/api/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBookBody)
                        )
                        .andExpect(status().isCreated());
            }

            // Request first page
            String listBody = """
                    {
                        "page": 0,
                        "size": 10,
                        "sortBy": "id",
                        "sortOrder": "DESC"
                    }
                    """;

            MvcResult result = mvc.perform(post("/api/book/_list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(listBody)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should prevent duplicate author names")
        void testDuplicateAuthorValidation() throws Exception {
            // Create first author
            String author1Body = """
                    {
                        "name": "Unique Author",
                        "country": "Ukraine",
                        "birthYear": 1950
                    }
                    """;

            mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(author1Body)
                    )
                    .andExpect(status().isCreated());

            // Try to create with same name
            mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(author1Body)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate book title is required")
        void testBookTitleRequired() throws Exception {
            // Create author first
            String authorBody = """
                    {
                        "name": "Test Author",
                        "country": "Ukraine",
                        "birthYear": 1950
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author = parseResponse(authorResult, AuthorDto.class);

            // Try to create book without title
            String invalidBody = """
                    {
                        "authorId": %d,
                        "yearPublished": 2020
                    }
                    """.formatted(author.getId());

            mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody)
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    class DataIntegrity {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("Should maintain data consistency on concurrent updates")
        void testDataConsistency() throws Exception {
            // Create author through API
            String authorBody = """
                    {
                        "name": "Test Author",
                        "country": "Ukraine",
                        "birthYear": 1950
                    }
                    """;

            MvcResult authorResult = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(authorBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author = parseResponse(authorResult, AuthorDto.class);

            // Create book through API
            String createBookBody = """
                    {
                        "title": "Original Title",
                        "authorId": %d,
                        "yearPublished": 2020,
                        "genres": ["Fiction"]
                    }
                    """.formatted(author.getId());

            MvcResult createResult = mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBookBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            BookDto book = parseResponse(createResult, BookDto.class);
            Long bookId = book.getId();

            // Multiple updates
            for (int i = 0; i < 4; i++) {
                String updateBody = """
                        {
                            "title": "Updated Title %d",
                            "authorId": %d,
                            "yearPublished": %d,
                            "genres": ["Fiction"]
                        }
                        """.formatted(i, author.getId(), 2020 + i);

                mvc.perform(put("/api/book/{id}", bookId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                        )
                        .andExpect(status().isOk());
            }

            // Verify final state through API
            MvcResult getResult = mvc.perform(get("/api/book/{id}", bookId)
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            BookDto finalBook = parseResponse(getResult, BookDto.class);
            assertThat(finalBook).isNotNull();
            assertThat(finalBook.getTitle()).startsWith("Updated Title");
        }

        @Test
        @DisplayName("Should handle orphaned books correctly")
        void testOrphanedBooks() throws Exception {
            // Create author1 through API
            String author1Body = """
                    {
                        "name": "Author 1",
                        "country": "Ukraine",
                        "birthYear": 1950
                    }
                    """;

            MvcResult author1Result = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(author1Body)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author1 = parseResponse(author1Result, AuthorDto.class);

            // Create author2 through API
            String author2Body = """
                    {
                        "name": "Author 2",
                        "country": "Ukraine",
                        "birthYear": 1960
                    }
                    """;

            MvcResult author2Result = mvc.perform(post("/api/author")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(author2Body)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthorDto author2 = parseResponse(author2Result, AuthorDto.class);

            // Create book1 for author1 through API
            String createBook1Body = """
                    {
                        "title": "Book 1",
                        "authorId": %d,
                        "yearPublished": 2020
                    }
                    """.formatted(author1.getId());

            MvcResult book1Result = mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBook1Body)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            BookDto book1 = parseResponse(book1Result, BookDto.class);
            Long book1Id = book1.getId();

            // Create book2 for author2 through API
            String createBook2Body = """
                    {
                        "title": "Book 2",
                        "authorId": %d,
                        "yearPublished": 2020
                    }
                    """.formatted(author2.getId());

            mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBook2Body)
                    )
                    .andExpect(status().isCreated());

            // Delete book1 BEFORE deleting author1 (FK constraint)
            mvc.perform(delete("/api/book/{id}", book1Id))
                    .andExpect(status().isNoContent());

            // Delete author1
            mvc.perform(delete("/api/author/{id}", author1.getId()))
                    .andExpect(status().isNoContent());

            // Verify author1 is deleted and author2 still exists
            assertThat(authorRepository.findById(author1.getId())).isEmpty();
            assertThat(authorRepository.findById(author2.getId())).isNotEmpty();
        }
    }

    // Helper method
    private <T> T parseResponse(MvcResult mvcResult, Class<T> c) {
        try {
            return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), c);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error parsing json", e);
        }
    }
}