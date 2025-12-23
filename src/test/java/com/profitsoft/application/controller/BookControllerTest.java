package com.profitsoft.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profitsoft.application.dto.BookDto;
import com.profitsoft.application.entities.Author;
import com.profitsoft.application.repository.AuthorRepository;
import com.profitsoft.application.repository.BookRepository;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Author testAuthor;

    @BeforeEach
    void setUp() {
        cleanUpDatabase();
        testAuthor = Author.builder()
                .name("Test Author")
                .country("Ukraine")
                .birthYear(1980)
                .build();
        testAuthor = authorRepository.save(testAuthor);
    }

    @AfterEach
    void tearDown() {
        cleanUpDatabase();
    }

    private void cleanUpDatabase() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    void testCreateBook() throws Exception {
        String title = "Test Book";
        int yearPublished = 2020;
        List<String> genres = List.of("Fiction", "Drama");

        String body = """
                {
                    "title": "%s",
                    "authorId": %d,
                    "yearPublished": %d,
                    "genres": %s
                }
                """.formatted(title, testAuthor.getId(), yearPublished,
                objectMapper.writeValueAsString(genres));

        MvcResult mvcResult = mvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isCreated())
                .andReturn();

        BookDto response = parseResponse(mvcResult, BookDto.class);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull().isGreaterThan(0);
        assertThat(response.getTitle()).isEqualTo(title);
        assertThat(response.getYearPublished()).isEqualTo(yearPublished);
        assertThat(response.getGenres()).containsAll(genres);
    }

    @Test
    void testCreateBook_validation() throws Exception {
        String body = """
                {
                    "authorId": %d
                }
                """.formatted(testAuthor.getId());

        mvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateBook_invalidAuthorId() throws Exception {
        String body = """
                {
                    "title": "Test Book",
                    "authorId": 99999,
                    "yearPublished": 2020
                }
                """;

        mvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetBook() throws Exception {
        String createBody = """
                {
                    "title": "Get Test Book",
                    "authorId": %d,
                    "yearPublished": 2021,
                    "genres": ["Mystery"]
                }
                """.formatted(testAuthor.getId());

        MvcResult createResult = mvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        BookDto createdBook = parseResponse(createResult, BookDto.class);
        Long bookId = createdBook.getId();

        // Now test GET
        MvcResult mvcResult = mvc.perform(get("/api/book/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        BookDto response = parseResponse(mvcResult, BookDto.class);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(bookId);
        assertThat(response.getTitle()).isEqualTo("Get Test Book");
        assertThat(response.getYearPublished()).isEqualTo(2021);
    }

    @Test
    void testGetBook_notFound() throws Exception {
        mvc.perform(get("/api/book/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBook() throws Exception {
        String createBody = """
                {
                    "title": "Original Title",
                    "authorId": %d,
                    "yearPublished": 2019,
                    "genres": ["Science"]
                }
                """.formatted(testAuthor.getId());

        MvcResult createResult = mvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        BookDto createdBook = parseResponse(createResult, BookDto.class);
        Long bookId = createdBook.getId();

        String newTitle = "Updated Title";
        int newYear = 2022;
        List<String> newGenres = List.of("Science", "Technology");

        String updateBody = """
                {
                    "title": "%s",
                    "authorId": %d,
                    "yearPublished": %d,
                    "genres": %s
                }
                """.formatted(newTitle, testAuthor.getId(), newYear,
                objectMapper.writeValueAsString(newGenres));

        MvcResult mvcResult = mvc.perform(put("/api/book/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                )
                .andExpect(status().isOk())
                .andReturn();

        BookDto response = parseResponse(mvcResult, BookDto.class);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(bookId);
        assertThat(response.getTitle()).isEqualTo(newTitle);
        assertThat(response.getYearPublished()).isEqualTo(newYear);
        assertThat(response.getGenres()).containsAll(newGenres);
    }

    @Test
    void testUpdateBook_notFound() throws Exception {
        String body = """
                {
                    "title": "Test",
                    "authorId": %d,
                    "yearPublished": 2020
                }
                """.formatted(testAuthor.getId());

        mvc.perform(put("/api/book/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteBook() throws Exception {
        String createBody = """
                {
                    "title": "Delete Test Book",
                    "authorId": %d,
                    "yearPublished": 2021
                }
                """.formatted(testAuthor.getId());

        MvcResult createResult = mvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        BookDto createdBook = parseResponse(createResult, BookDto.class);
        Long bookId = createdBook.getId();

        mvc.perform(delete("/api/book/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        assertThat(bookRepository.findById(bookId)).isEmpty();
    }

    @Test
    void testDeleteBook_notFound() throws Exception {
        mvc.perform(delete("/api/book/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void testListBooks() throws Exception {
        for (int i = 0; i < 5; i++) {
            String createBody = """
                    {
                        "title": "Book %d",
                        "authorId": %d,
                        "yearPublished": %d,
                        "genres": ["Genre%d"]
                    }
                    """.formatted(i, testAuthor.getId(), 2020 + i, i);

            mvc.perform(post("/api/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody))
                    .andExpect(status().isCreated());
        }

        String body = """
                {
                    "page": 0,
                    "size": 10,
                    "sortBy": "id",
                    "sortOrder": "DESC"
                }
                """;

        mvc.perform(post("/api/book/_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isOk());
    }

    @Test
    void testUploadBooks() throws Exception {
        String jsonContent = """
                [
                  {
                    "title": "Uploaded Book",
                    "author": { "name": "Test Author" },
                    "year_published": 2020,
                    "genre": ["Fiction"]
                  }
                ]
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.json",
                "application/json",
                jsonContent.getBytes()
        );

        mvc.perform(multipart("/api/book/upload").file(file))
                .andExpect(status().isCreated());
    }

    @Test
    void testGenerateReport() throws Exception {
        String createBody = """
                {
                    "title": "Report Book",
                    "authorId": %d,
                    "yearPublished": 2021,
                    "genres": ["Test"]
                }
                """.formatted(testAuthor.getId());

        mvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/book/_report")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    private <T> T parseResponse(MvcResult mvcResult, Class<T> c) {
        try {
            return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), c);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error parsing json", e);
        }
    }
}