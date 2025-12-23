package com.profitsoft.application.service;

import com.profitsoft.application.dto.*;
import com.profitsoft.application.entities.Author;
import com.profitsoft.application.entities.Book;
import com.profitsoft.application.exceptions.ResourceNotFoundException;
import com.profitsoft.application.mapper.BookMapper;
import com.profitsoft.application.repository.BookRepository;
import com.profitsoft.application.spec.BookSpecification;
import com.profitsoft.application.utils.BookJsonParser;
import com.profitsoft.application.utils.CsvExporter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final AuthorService authorService;
    private final BookRepository bookRepository;
    private final BookJsonParser bookJsonParser;
    private final BookMapper bookMapper;

    public BookDto create(BookCreateDto dto) {
        Author author = authorService.findEntityById(dto.getAuthorId());
        Book book = Book.builder()
                .title(dto.getTitle())
                .yearPublished(dto.getYearPublished())
                .genres(dto.getGenres() != null ? dto.getGenres() : new ArrayList<>())
                .author(author)
                .build();
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    @Transactional(readOnly = true)
    public BookDto findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with id: " + id));
        return bookMapper.toDto(book);
    }

    public BookDto update(Long id, BookCreateDto dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with id: " + id));
        Author author = authorService.findEntityById(dto.getAuthorId());
        book.setTitle(dto.getTitle());
        book.setYearPublished(dto.getYearPublished());
        book.setGenres(dto.getGenres() != null ? dto.getGenres() : new ArrayList<>());
        book.setAuthor(author);
        Book updated = bookRepository.save(book);
        return bookMapper.toDto(updated);
    }

    public void delete(Long id) {
        bookRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookListItemDto> list(BookListRequest req) {
        Sort.Direction direction = req.getSortOrder() != null
                && req.getSortOrder().equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortField = (req.getSortBy() != null && !req.getSortBy().isEmpty())
                ? req.getSortBy()
                : "id";
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(),
                Sort.by(direction, sortField));
        Specification<Book> spec = Specification.where(BookSpecification.authorId(req.getAuthorId()))
                .and(BookSpecification.genreLike(req.getGenre()))
                .and(BookSpecification.titleLike(req.getTitle()));
        Page<Book> page = bookRepository.findAll(spec, pageable);
        List<BookListItemDto> items = page.getContent().stream()
                .map(bookMapper::toListItemDto)
                .toList();
        return new PageResponse<>(items, page.getTotalPages(), page.getTotalElements(),
                req.getPage(), req.getSize());
    }

    public BookImportResultDto uploadFromFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/json")) {
            throw new IllegalArgumentException("File must be JSON format. Received: " + contentType);
        }
        try {
            File tempFile = File.createTempFile("books_", ".json");
            file.transferTo(tempFile);
            Path path = tempFile.toPath();
            List<Object> importedBooks = new ArrayList<>();
            bookJsonParser.parseFileAsPojo(path, pojo -> {
                try {
                    Long authorId = pojo.getAuthorIdValue();
                    if (authorId == null) {
                        log.warn("Failed to import book '{}': No authorId found", pojo.getTitle());
                        return;
                    }
                    Author author = authorService.findEntityById(authorId);
                    Integer yearPub = pojo.getYearPublished();
                    if (yearPub == null) {
                        yearPub = pojo.getYear_published();
                    }
                    Book book = Book.builder()
                            .title(pojo.getTitle())
                            .yearPublished(yearPub)
                            .genres(pojo.getGenres() != null ? pojo.getGenres() : new ArrayList<>())
                            .author(author)
                            .build();
                    bookRepository.save(book);
                    importedBooks.add(book);
                    log.info("Successfully imported book: {}", pojo.getTitle());
                } catch (Exception e) {
                    log.warn("Failed to import book: {}", e.getMessage());
                }
            });
            Files.delete(path);
            BookImportResultDto result = new BookImportResultDto();
            result.setSuccessCount(importedBooks.size());
            result.setFailedCount(0);
            return result;
        } catch (IOException e) {
            log.error("Error processing file", e);
            throw new IOException("Error processing file: " + e.getMessage(), e);
        }
    }

    @Transactional
    public BookImportResultDto importFromJson(List<BookPojo> books) {
        int success = 0;
        int failed = 0;
        for (BookPojo pojo : books) {
            try {
                String authorName = pojo.getAuthorName();
                if (authorName == null || authorName.isBlank()) {
                    failed++;
                    continue;
                }
                Author author = authorService.findByName(authorName)
                        .orElseThrow(() -> new IllegalArgumentException("Author not found: " + authorName));
                Book book = Book.builder()
                        .title(pojo.getTitle())
                        .yearPublished(pojo.getYearPublished())
                        .genres(pojo.getGenres() != null ? pojo.getGenres() : new ArrayList<>())
                        .author(author)
                        .build();
                bookRepository.save(book);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to import a book: {}", e.getMessage());
            }
        }
        BookImportResultDto res = new BookImportResultDto();
        res.setSuccessCount(success);
        res.setFailedCount(failed);
        return res;
    }

    @Transactional(readOnly = true)
    public byte[] generateCsvReport(BookListRequest req) {
        if (req == null) {
            req = new BookListRequest();
            req.setSize(100);
            req.setSortBy("id");
            req.setSortOrder("DESC");
        }
        if (req.getSize() <= 0 || req.getSize() > 1000) {
            req.setSize(100);
        }
        Sort.Direction direction = req.getSortOrder() != null
                && req.getSortOrder().equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortField = (req.getSortBy() != null && !req.getSortBy().isEmpty())
                ? req.getSortBy()
                : "id";
        Pageable pageable = PageRequest.of(0, req.getSize(), Sort.by(direction, sortField));
        Specification<Book> spec = Specification.where(BookSpecification.authorId(req.getAuthorId()))
                .and(BookSpecification.genreLike(req.getGenre()))
                .and(BookSpecification.titleLike(req.getTitle()));
        Page<Book> page = bookRepository.findAll(spec, pageable);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CsvExporter.writeBooksToCsv(page.getContent(), out);
            log.info("Generated CSV report with {} books", page.getContent().size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export CSV: " + e.getMessage(), e);
        }
    }
}