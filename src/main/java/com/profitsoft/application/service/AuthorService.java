package com.profitsoft.application.service;

import com.profitsoft.application.dto.AuthorDto;
import com.profitsoft.application.entities.Author;
import com.profitsoft.application.repository.AuthorRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private final AuthorRepository repo;

    public List<AuthorDto> findAll() {
        return repo
                .findAll().stream()
                .map(this::toDto)
                .collect(Collectors
                        .toList());
    }

    @Transactional
    public AuthorDto create(AuthorDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Author name cannot be empty");
        }
        if (dto.getBirthYear() != null && dto.getBirthYear() > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("Birth year cannot be in the future");
        }
        if (repo.existsByNameIgnoreCase(dto.getName()))
            throw new IllegalArgumentException("Author with this name already exists");
        Author a = Author.builder().name(dto.getName())
                .country(dto.getCountry())
                .birthYear(dto.getBirthYear()).build();
        a = repo.save(a);
        return toDto(a);
    }

    @Transactional
    public AuthorDto update(Long id, AuthorDto dto) {
        Author author = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Author not found"));
        if (!author.getName().equalsIgnoreCase(dto.getName()) && repo.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Author already exists");
        }
        author.setName(dto.getName());
        author.setCountry(dto.getCountry());
        author.setBirthYear(dto.getBirthYear());
        return toDto(repo.save(author));
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Author findEntityById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Author not found with id: " + id));
    }

    public Optional<Author> findByName(String name) {
        return repo.findByNameIgnoreCase(name);
    }

    private AuthorDto toDto(Author a) {
        if (a == null) {
            return null;
        }
        AuthorDto d = new AuthorDto();
        d.setId(a.getId());
        d.setName(a.getName());
        d.setCountry(a.getCountry());
        d.setBirthYear(a.getBirthYear());
        return d;
    }
}
