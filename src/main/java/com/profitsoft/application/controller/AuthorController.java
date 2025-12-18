package com.profitsoft.application.controller;

import com.profitsoft.application.dto.AuthorDto;
import com.profitsoft.application.service.AuthorService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "http://localhost:3050")
@RestController
@RequestMapping("/api/author")
@RequiredArgsConstructor
public class AuthorController {
    private final AuthorService service;

    @GetMapping
    public ResponseEntity<List<AuthorDto>> all() {
        log.info("Getting all authors");
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<AuthorDto> create(@Validated @RequestBody AuthorDto dto) {
        log.info("Creating author: {}", dto.getName());
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorDto> update(@PathVariable Long id,
                                            @Validated @RequestBody AuthorDto dto) {
        log.info("Updating author with id: {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting author with id: {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
