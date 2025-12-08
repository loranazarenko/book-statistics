package com.profitsoft.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profitsoft.application.dto.AuthorDto;
import com.profitsoft.application.entities.Author;
import com.profitsoft.application.repository.AuthorRepository;
import com.profitsoft.application.repository.BookRepository;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Integration tests for Author Controller using H2 in-memory database
 * Uses embedded H2 for faster tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthorControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

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

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCreateAuthor() throws Exception {
        String name = "Taras Shevchenko";
        String country = "Ukraine";
        int birthYear = 1814;

        String body = """
                  {
                      "name": "%s",
                      "country": "%s",
                      "birthYear": %d
                  }
                """.formatted(name, country, birthYear);

        MvcResult mvcResult = mvc.perform(post("/api/author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isOk())
                .andReturn();

        AuthorDto response = parseResponse(mvcResult, AuthorDto.class);
        assertThat(response.getId()).isNotNull().isGreaterThan(0);
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getCountry()).isEqualTo(country);
        assertThat(response.getBirthYear()).isEqualTo(birthYear);

        // Verify in database
        Author author = authorRepository.findById(response.getId()).orElse(null);
        assertThat(author).isNotNull();
        assertThat(author.getName()).isEqualTo(name);
        assertThat(author.getCountry()).isEqualTo(country);
    }

    @Test
    void testCreateAuthor_validation_emptyName() throws Exception {
        String body = """
                  {
                      "name": "",
                      "country": "Ukraine",
                      "birthYear": 1900
                  }
                """;

        mvc.perform(post("/api/author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAuthor_validation_missingName() throws Exception {
        String body = """
                  {
                      "country": "Ukraine",
                      "birthYear": 1900
                  }
                """;

        mvc.perform(post("/api/author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAuthor_validation_futureYear() throws Exception {
        int futureYear = java.time.LocalDate.now().getYear() + 10;
        String body = """
                  {
                      "name": "Future Author",
                      "country": "Ukraine",
                      "birthYear": %d
                  }
                """.formatted(futureYear);

        mvc.perform(post("/api/author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAuthor_duplicate() throws Exception {
        // Create first author
        Author author1 = Author.builder()
                .name("Duplicate Author")
                .country("Ukraine")
                .birthYear(1900)
                .build();
        authorRepository.save(author1);

        // Try to create duplicate
        String body = """
                  {
                      "name": "Duplicate Author",
                      "country": "Ukraine",
                      "birthYear": 1905
                  }
                """;

        mvc.perform(post("/api/author")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllAuthors() throws Exception {
        // Create multiple authors
        for (int i = 0; i < 3; i++) {
            Author author = Author.builder()
                    .name("Author " + i)
                    .country("Country " + i)
                    .birthYear(1900 + i)
                    .build();
            authorRepository.save(author);
        }

        MvcResult mvcResult = mvc.perform(get("/api/author")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        AuthorDto[] response = parseResponse(mvcResult, AuthorDto[].class);
        assertThat(response).hasSize(3);
        assertThat(response[0].getName()).contains("Author");
    }

    @Test
    void testGetAllAuthors_empty() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/api/author")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        AuthorDto[] response = parseResponse(mvcResult, AuthorDto[].class);
        assertThat(response).isEmpty();
    }

    @Test
    void testUpdateAuthor() throws Exception {
        // Create an author first
        Author author = Author.builder()
                .name("Original Name")
                .country("Ukraine")
                .birthYear(1950)
                .build();
        author = authorRepository.save(author);

        String newName = "Updated Name";
        String newCountry = "Poland";
        int newBirthYear = 1960;

        String body = """
                  {
                      "name": "%s",
                      "country": "%s",
                      "birthYear": %d
                  }
                """.formatted(newName, newCountry, newBirthYear);

        MvcResult mvcResult = mvc.perform(put("/api/author/{id}", author.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isOk())
                .andReturn();

        AuthorDto response = parseResponse(mvcResult, AuthorDto.class);
        assertThat(response.getId()).isEqualTo(author.getId());
        assertThat(response.getName()).isEqualTo(newName);
        assertThat(response.getCountry()).isEqualTo(newCountry);
        assertThat(response.getBirthYear()).isEqualTo(newBirthYear);

        // Verify in database
        Author updated = authorRepository.findById(author.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getCountry()).isEqualTo(newCountry);
    }

    @Test
    void testUpdateAuthor_notFound() throws Exception {
        String body = """
                  {
                      "name": "Test Author",
                      "country": "Ukraine",
                      "birthYear": 1900
                  }
                """;

        mvc.perform(put("/api/author/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateAuthor_duplicateName() throws Exception {
        // Create two authors
        Author author1 = Author.builder()
                .name("Author One")
                .country("Ukraine")
                .birthYear(1900)
                .build();
        author1 = authorRepository.save(author1);

        Author author2 = Author.builder()
                .name("Author Two")
                .country("Ukraine")
                .birthYear(1910)
                .build();
        author2 = authorRepository.save(author2);

        // Try to update author2 with author1's name
        String body = """
                  {
                      "name": "Author One",
                      "country": "Ukraine",
                      "birthYear": 1920
                  }
                """;

        mvc.perform(put("/api/author/{id}", author2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteAuthor() throws Exception {
        // Create an author first
        Author author = Author.builder()
                .name("Delete Test Author")
                .country("Ukraine")
                .birthYear(1950)
                .build();
        author = authorRepository.save(author);

        mvc.perform(delete("/api/author/{id}", author.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        // Verify deleted from database
        assertThat(authorRepository.findById(author.getId())).isEmpty();
    }

    @Test
    void testDeleteAuthor_notFound() throws Exception {
        mvc.perform(delete("/api/author/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());
    }

    private <T> T parseResponse(MvcResult mvcResult, Class<T> c) {
        try {
            return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), c);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Error parsing json", e);
        }
    }
}