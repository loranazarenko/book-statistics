package com.profitsoft.application.controller;

import com.profitsoft.application.dto.*;
import com.profitsoft.application.service.BookService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@CrossOrigin(origins = "http://localhost:3050")
@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService service;

    @PostMapping
    public ResponseEntity<BookDto> create(@RequestBody @Valid BookCreateDto dto) {
        log.info("Creating book with title: {}", dto.getTitle());
        BookDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> get(@PathVariable Long id) {
        log.info("Getting book with id: {}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDto> update(@PathVariable Long id,
                                          @Validated @RequestBody BookCreateDto dto) {
        log.info("Updating book with id: {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting book with id: {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/_list")
    public ResponseEntity<PageResponse<BookListItemDto>> list(@RequestBody BookListRequest req) {
        log.info("Listing books with filters: page={}, size={}, title={}, authorId={}",
                req.getPage(), req.getSize(), req.getTitle(), req.getAuthorId());
        return ResponseEntity.ok(service.list(req));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<BookImportResultDto> upload(@RequestParam("file") MultipartFile file)
            throws IOException {
        log.info("Uploading books from file: {}", file.getOriginalFilename());
        BookImportResultDto result = service.uploadFromFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/_report")
    public void generateReport(@RequestBody(required = false) BookListRequest req,
                               HttpServletResponse response) {
        try {
            log.info("Generating CSV report");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=books_report.csv");
            byte[] csvData = service.generateCsvReport(req);
            response.getOutputStream().write(csvData);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("Error generating CSV report", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error generating report: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Error writing error response", ex);
            }
        }
    }
}
