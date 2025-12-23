package com.profitsoft.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.profitsoft.application.dto.*;
import com.profitsoft.application.entities.Author;
import com.profitsoft.application.entities.Book;
import com.profitsoft.application.exceptions.ResourceNotFoundException;
import com.profitsoft.application.mapper.BookMapper;
import com.profitsoft.application.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepo;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AuthorService authorService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookService bookService;

    private Author testAuthor;
    private Book testBook;
    private BookCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testAuthor = Author.builder()
                .id(1L)
                .name("Test Author")
                .country("Ukraine")
                .birthYear(1950)
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .yearPublished(2020)
                .author(testAuthor)
                .genres(List.of("Fiction"))
                .build();

        testCreateDto = new BookCreateDto();
        testCreateDto.setTitle("Test Book");
        testCreateDto.setAuthorId(1L);
        testCreateDto.setYearPublished(2020);
        testCreateDto.setGenres(List.of("Fiction"));
    }

    @BeforeEach
    void setup() {
        lenient().when(bookMapper.toDto(any(Book.class)))
                .thenAnswer(invocation -> {
                    Book book = invocation.getArgument(0);
                    if (book == null) return null;

                    BookDto dto = new BookDto();
                    dto.setId(book.getId());
                    dto.setTitle(book.getTitle());
                    dto.setYearPublished(book.getYearPublished());
                    dto.setGenres(book.getGenres());

                    // Map author
                    if (book.getAuthor() != null) {
                        AuthorDto authorDto = new AuthorDto();
                        authorDto.setId(book.getAuthor().getId());
                        authorDto.setName(book.getAuthor().getName());
                        authorDto.setCountry(book.getAuthor().getCountry());
                        authorDto.setBirthYear(book.getAuthor().getBirthYear());
                        dto.setAuthor(authorDto);
                    }

                    return dto;
                });

        lenient().when(bookMapper.toListItemDto(any(Book.class)))
                .thenAnswer(invocation -> {
                    Book book = invocation.getArgument(0);
                    if (book == null) return null;

                    BookListItemDto dto = new BookListItemDto();
                    dto.setId(book.getId());
                    dto.setTitle(book.getTitle());
                    dto.setYearPublished(book.getYearPublished());
                    if (book.getAuthor() != null) {
                        dto.setAuthorName(book.getAuthor().getName());
                    }
                    return dto;
                });
    }

    @Test
    void testCreateBook_success() {
        when(authorService.findEntityById(1L)).thenReturn(testAuthor);
        when(bookRepo.save(any(Book.class))).thenReturn(testBook);

        BookDto result = bookService.create(testCreateDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Book");
        assertThat(result.getYearPublished()).isEqualTo(2020);
        assertThat(result.getAuthor().getId()).isEqualTo(1L);
        verify(authorService, times(1)).findEntityById(1L);
        verify(bookRepo, times(1)).save(any(Book.class));
    }

    @Test
    void testCreateBook_authorNotFound() {
        when(authorService.findEntityById(99L))
                .thenThrow(new ResourceNotFoundException("Author", "id", 99L));

        BookCreateDto dto = new BookCreateDto();
        dto.setTitle("Test Book");
        dto.setAuthorId(99L);
        dto.setYearPublished(2020);

        assertThatThrownBy(() -> bookService.create(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
    }

    @Test
    void testFindById_success() {
        when(bookRepo.findById(1L)).thenReturn(Optional.of(testBook));

        BookDto result = bookService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Book");
        verify(bookRepo, times(1)).findById(1L);
    }

    @Test
    void testFindById_notFound() {
        when(bookRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
    }

    @Test
    void testUpdateBook_success() {
        BookCreateDto updateDto = new BookCreateDto();
        updateDto.setTitle("Updated Book");
        updateDto.setAuthorId(1L);
        updateDto.setYearPublished(2021);
        updateDto.setGenres(List.of("Drama"));

        Book updatedBook = Book.builder()
                .id(1L)
                .title("Updated Book")
                .yearPublished(2021)
                .author(testAuthor)
                .genres(List.of("Drama"))
                .build();

        when(bookRepo.findById(1L)).thenReturn(Optional.of(testBook));
        when(authorService.findEntityById(1L)).thenReturn(testAuthor);
        when(bookRepo.save(any(Book.class))).thenReturn(updatedBook);

        BookDto result = bookService.update(1L, updateDto);

        assertThat(result.getTitle()).isEqualTo("Updated Book");
        assertThat(result.getYearPublished()).isEqualTo(2021);
        verify(bookRepo, times(1)).findById(1L);
        verify(bookRepo, times(1)).save(any(Book.class));
    }

    @Test
    void testUpdateBook_notFound() {
        when(bookRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(99L, testCreateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
    }

    @Test
    void testDeleteBook_success() {
        doNothing().when(bookRepo).deleteById(1L);

        bookService.delete(1L);

        verify(bookRepo, times(1)).deleteById(1L);
    }

    @Test
    void testCreateBook_withoutGenres() {
        BookCreateDto dto = new BookCreateDto();
        dto.setTitle("Book without genres");
        dto.setAuthorId(1L);
        dto.setYearPublished(2020);
        dto.setGenres(null);

        Book savedBook = Book.builder()
                .id(1L)
                .title("Book without genres")
                .yearPublished(2020)
                .author(testAuthor)
                .genres(List.of())
                .build();

        when(authorService.findEntityById(1L)).thenReturn(testAuthor);
        when(bookRepo.save(any(Book.class))).thenReturn(savedBook);

        BookDto result = bookService.create(dto);

        assertThat(result.getTitle()).isEqualTo("Book without genres");
        assertThat(result.getGenres()).isEmpty();
    }

    @Test
    void testCreateBook_withMultipleGenres() {
        List<String> genres = List.of("Fiction", "Drama", "Mystery", "Adventure");
        BookCreateDto dto = new BookCreateDto();
        dto.setTitle("Multi-genre book");
        dto.setAuthorId(1L);
        dto.setYearPublished(2020);
        dto.setGenres(genres);

        Book savedBook = Book.builder()
                .id(1L)
                .title("Multi-genre book")
                .yearPublished(2020)
                .author(testAuthor)
                .genres(genres)
                .build();

        when(authorService.findEntityById(1L)).thenReturn(testAuthor);
        when(bookRepo.save(any(Book.class))).thenReturn(savedBook);

        BookDto result = bookService.create(dto);

        assertThat(result.getGenres()).hasSize(4).containsAll(genres);
    }

    @Test
    void testGenerateCsvReport() {
        BookListRequest req = new BookListRequest();
        req.setSize(100);
        req.setSortBy("id");
        req.setSortOrder("ASC");

        Page<Book> mockPage = new PageImpl<>(List.of(testBook));

        when(bookRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        byte[] result = bookService.generateCsvReport(req);

        assertThat(result).isNotEmpty();
        assertThat(result).isNotNull();

        String csvContent = new String(result);
        assertThat(csvContent).contains("ID,Title,Author,Year Published,Genres");
        assertThat(csvContent).contains(testBook.getTitle());
        assertThat(csvContent).contains(testAuthor.getName());

        verify(bookRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGenerateCsvReport_emptyList() {
        BookListRequest req = new BookListRequest();
        req.setSize(100);
        req.setSortBy("id");
        req.setSortOrder("ASC");

        Page<Book> emptyPage = new PageImpl<>(List.of());
        when(bookRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);
        byte[] result = bookService.generateCsvReport(req);
        assertThat(result).isNotEmpty();
        assertThat(result).isNotNull();

        String csvContent = new String(result);
        assertThat(csvContent).contains("ID,Title,Author,Year Published,Genres");
        assertThat(csvContent).doesNotContain("1,");

        verify(bookRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testUploadFromFile_success() throws IOException {
        // Prepare test data
        BookPojo pojo1 = new BookPojo();
        pojo1.setTitle("Book 1");
        pojo1.setAuthor("Test Author");
        pojo1.setYearPublished(2020);
        pojo1.setGenres(List.of("Fiction"));

        BookPojo pojo2 = new BookPojo();
        pojo2.setTitle("Book 2");
        pojo2.setAuthor("Test Author");
        pojo2.setYearPublished(2021);
        pojo2.setGenres(List.of("Drama"));

        List<BookPojo> pojos = List.of(pojo1, pojo2);

        // Mock the file content
        String jsonContent = """
                [
                  {
                    "title": "Book 1",
                    "author": "Test Author",
                    "yearPublished": 2020,
                    "genres": ["Fiction"]
                  },
                  {
                    "title": "Book 2",
                    "author": "Test Author",
                    "yearPublished": 2021,
                    "genres": ["Drama"]
                  }
                ]
                """;

        // Mock author service and repository
        Book savedBook1 = Book.builder()
                .id(1L)
                .title("Book 1")
                .yearPublished(2020)
                .author(testAuthor)
                .genres(List.of("Fiction"))
                .build();

        Book savedBook2 = Book.builder()
                .id(2L)
                .title("Book 2")
                .yearPublished(2021)
                .author(testAuthor)
                .genres(List.of("Drama"))
                .build();

        when(authorService.findByName("Test Author")).thenReturn(Optional.of(testAuthor));
        when(bookRepo.save(any(Book.class))).thenReturn(savedBook1).thenReturn(savedBook2);

        // Execute
        BookImportResultDto result = bookService.importFromJson(pojos);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailedCount()).isEqualTo(0);
        verify(authorService, times(2)).findByName("Test Author");
        verify(bookRepo, times(2)).save(any(Book.class));
    }

    @Test
    void testUploadFromFile_invalidJsonSyntax() throws IOException {
        String invalidJson = "{ invalid json }";

        // Mock ObjectMapper to throw JsonParseException
        when(objectMapper.readValue(invalidJson, BookPojo[].class))
                .thenThrow(new JsonParseException(null, "Invalid JSON"));

        // This should handle the exception gracefully
        assertThatThrownBy(() -> {
            try {
                objectMapper.readValue(invalidJson, BookPojo[].class);
            } catch (JsonParseException e) {
                throw new IllegalArgumentException("Invalid JSON syntax: " + e.getOriginalMessage());
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON syntax");
    }

    @Test
    void testUploadFromFile_invalidJsonStructure() throws IOException {
        String invalidStructure = """
                [
                  {
                    "title": 123,
                    "author": "Test Author",
                    "yearPublished": "not a number"
                  }
                ]
                """;


        // Mock ObjectMapper to throw JsonMappingException
        when(objectMapper.readValue(invalidStructure, BookPojo[].class))
                .thenThrow(new JsonMappingException(null, "Invalid structure"));

        // This should handle the exception gracefully
        assertThatThrownBy(() -> {
            try {
                objectMapper.readValue(invalidStructure, BookPojo[].class);
            } catch (JsonMappingException e) {
                throw new IllegalArgumentException("Invalid JSON structure: " + e.getOriginalMessage());
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON structure");
    }

    @Test
    void testUploadFromFile_emptyFile() throws IOException {
        BookImportResultDto result = bookService.importFromJson(List.of());

        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailedCount()).isEqualTo(0);
    }

    @Test
    void testUploadFromFile_partialFailure() throws IOException {
        BookPojo validPojo = new BookPojo();
        validPojo.setTitle("Valid Book");
        validPojo.setAuthor("Test Author");
        validPojo.setYearPublished(2020);

        BookPojo invalidPojo = new BookPojo();
        invalidPojo.setTitle("Invalid Book");
        invalidPojo.setAuthor("NonExistent Author");
        invalidPojo.setYearPublished(2021);

        List<BookPojo> pojos = List.of(validPojo, invalidPojo);

        Book savedBook = Book.builder()
                .id(1L)
                .title("Valid Book")
                .yearPublished(2020)
                .author(testAuthor)
                .build();

        // First call returns the author, second returns empty
        when(authorService.findByName("Test Author")).thenReturn(Optional.of(testAuthor));
        when(authorService.findByName("NonExistent Author")).thenReturn(Optional.empty());
        when(bookRepo.save(any(Book.class))).thenReturn(savedBook);

        BookImportResultDto result = bookService.importFromJson(pojos);

        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testImportFromJson_success() {
        BookPojo pojo = new BookPojo();
        pojo.setTitle("Imported Book");
        pojo.setAuthor("Test Author");
        pojo.setYearPublished(2020);
        pojo.setGenres(List.of("Fiction"));

        Book savedBook = Book.builder()
                .id(1L)
                .title("Imported Book")
                .yearPublished(2020)
                .author(testAuthor)
                .genres(List.of("Fiction"))
                .build();

        when(authorService.findByName("Test Author")).thenReturn(Optional.of(testAuthor));
        when(bookRepo.save(any(Book.class))).thenReturn(savedBook);

        BookImportResultDto result = bookService.importFromJson(List.of(pojo));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(0);
        verify(authorService, times(1)).findByName("Test Author");
    }

    @Test
    void testImportFromJson_authorNotFound() {
        BookPojo pojo = new BookPojo();
        pojo.setTitle("Imported Book");
        pojo.setAuthor("NonExistent Author");
        pojo.setYearPublished(2020);

        when(authorService.findByName("NonExistent Author")).thenReturn(Optional.empty());

        BookImportResultDto result = bookService.importFromJson(List.of(pojo));

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testImportFromJson_emptyAuthorName() {
        BookPojo pojo = new BookPojo();
        pojo.setTitle("Imported Book");
        pojo.setAuthor(null);
        pojo.setYearPublished(2020);

        BookImportResultDto result = bookService.importFromJson(List.of(pojo));

        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailedCount()).isEqualTo(1);
    }

    @Test
    void testImportFromJson_multipleBooks() {
        List<BookPojo> pojos = List.of(
                createTestPojo("Book 1", "Test Author"),
                createTestPojo("Book 2", "Test Author"),
                createTestPojo("Book 3", "Test Author")
        );

        when(authorService.findByName("Test Author")).thenReturn(Optional.of(testAuthor));
        when(bookRepo.save(any(Book.class))).thenReturn(testBook);

        BookImportResultDto result = bookService.importFromJson(pojos);

        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailedCount()).isEqualTo(0);
        verify(bookRepo, times(3)).save(any(Book.class));
    }

    private BookPojo createTestPojo(String title, String authorName) {
        BookPojo pojo = new BookPojo();
        pojo.setTitle(title);
        pojo.setAuthor(authorName);
        pojo.setYearPublished(2020);
        return pojo;
    }
}