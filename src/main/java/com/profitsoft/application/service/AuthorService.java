package com.profitsoft.application.service;

import com.profitsoft.application.dto.AuthorDto;
import com.profitsoft.application.entities.Author;
import com.profitsoft.application.exceptions.ResourceNotFoundException;
import com.profitsoft.application.mapper.AuthorMapper;
import com.profitsoft.application.repository.AuthorRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    public List<AuthorDto> findAll() {
        return authorRepository.findAll()
                .stream()
                .map(authorMapper::toDto)
                .toList();
    }

    public AuthorDto findById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Author not found with id: " + id));
        return authorMapper.toDto(author);
    }

    public Author findEntityById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Author not found with id: " + id));
    }

    public Optional<Author> findByName(String name) {
        return authorRepository.findByNameIgnoreCase(name);
    }

    @Transactional
    public AuthorDto create(AuthorDto authorDto) {
        if (authorDto.getName() == null || authorDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Author name cannot be empty");
        }

        if (authorDto.getBirthYear() != null
                && authorDto.getBirthYear() > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("Birth year cannot be in the future");
        }

        if (authorRepository.existsByNameIgnoreCase(authorDto.getName())) {
            throw new IllegalArgumentException("Author with this name already exists");
        }

        Author author = authorMapper.toEntity(authorDto);
        Author savedAuthor = authorRepository.save(author);
        return authorMapper.toDto(savedAuthor);
    }

    @Transactional
    public AuthorDto update(Long id, AuthorDto authorDto) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Author not found with id: " + id));

        if (!author.getName().equalsIgnoreCase(authorDto.getName())
                && authorRepository.existsByNameIgnoreCase(authorDto.getName())) {
            throw new IllegalArgumentException("Author with this name already exists");
        }

        author.setName(authorDto.getName());
        author.setCountry(authorDto.getCountry());
        author.setBirthYear(authorDto.getBirthYear());

        Author updatedAuthor = authorRepository.save(author);
        return authorMapper.toDto(updatedAuthor);
    }

    @Transactional
    public void delete(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Author not found with id: " + id);
        }
        authorRepository.deleteById(id);
    }
}