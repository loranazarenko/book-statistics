package com.profitsoft.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.profitsoft.application.dto.AuthorDto;
import com.profitsoft.application.entities.Author;
import com.profitsoft.application.exceptions.ResourceNotFoundException;
import com.profitsoft.application.mapper.AuthorMapper;
import com.profitsoft.application.repository.AuthorRepository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorService authorService;

    private Author testAuthor;
    private AuthorDto testAuthorDto;

    @BeforeEach
    void setUp() {
        testAuthor = Author.builder()
                .id(1L)
                .name("Test Author")
                .country("Ukraine")
                .birthYear(1950)
                .build();

        testAuthorDto = new AuthorDto();
        testAuthorDto.setId(1L);
        testAuthorDto.setName("Test Author");
        testAuthorDto.setCountry("Ukraine");
        testAuthorDto.setBirthYear(1950);
    }

    @BeforeEach
    void setupMapper() {
        lenient().when(authorMapper.toDto(any(Author.class)))
                .thenAnswer(invocation -> {
                    Author author = invocation.getArgument(0);
                    if (author == null) return null;

                    AuthorDto dto = new AuthorDto();
                    dto.setId(author.getId());
                    dto.setName(author.getName());
                    dto.setCountry(author.getCountry());
                    dto.setBirthYear(author.getBirthYear());
                    return dto;
                });

        lenient().when(authorMapper.toEntity(any(AuthorDto.class)))
                .thenAnswer(invocation -> {
                    AuthorDto dto = invocation.getArgument(0);
                    if (dto == null) return null;

                    return Author.builder()
                            .id(dto.getId())
                            .name(dto.getName())
                            .country(dto.getCountry())
                            .birthYear(dto.getBirthYear())
                            .build();
                });
    }

    @Test
    void testCreateAuthor_success() {
        AuthorDto dto = new AuthorDto();
        dto.setName("New Author");
        dto.setCountry("Ukraine");
        dto.setBirthYear(1980);

        Author savedAuthor = Author.builder()
                .id(1L)
                .name("New Author")
                .country("Ukraine")
                .birthYear(1980)
                .build();

        when(authorRepository.existsByNameIgnoreCase("New Author")).thenReturn(false);
        when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);

        AuthorDto result = authorService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New Author");
        assertThat(result.getCountry()).isEqualTo("Ukraine");
        assertThat(result.getBirthYear()).isEqualTo(1980);
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void testCreateAuthor_validation_emptyName() {
        AuthorDto dto = new AuthorDto();
        dto.setName("");
        dto.setCountry("Ukraine");
        dto.setBirthYear(1950);

        assertThatThrownBy(() -> authorService.create(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAuthor_validation_futureYear() {
        int futureYear = java.time.LocalDate.now().getYear() + 10;
        AuthorDto dto = new AuthorDto();
        dto.setName("Future Author");
        dto.setCountry("Ukraine");
        dto.setBirthYear(futureYear);

        assertThatThrownBy(() -> authorService.create(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetAllAuthors_success() {
        List<Author> authors = List.of(
                testAuthor,
                Author.builder().id(2L).name("Author 2").country("Poland").birthYear(1960).build()
        );

        when(authorRepository.findAll()).thenReturn(authors);

        List<AuthorDto> results = authorService.findAll();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Test Author");
        verify(authorRepository, times(1)).findAll();
    }

    @Test
    void testGetAllAuthors_empty() {
        when(authorRepository.findAll()).thenReturn(List.of());

        List<AuthorDto> results = authorService.findAll();

        assertThat(results).isEmpty();
        verify(authorRepository, times(1)).findAll();
    }

    @Test
    void testFindByName_success() {
        when(authorRepository.findByNameIgnoreCase("Test Author")).thenReturn(Optional.of(testAuthor));

        Optional<Author> result = authorService.findByName("Test Author");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Author");
        verify(authorRepository, times(1)).findByNameIgnoreCase("Test Author");
    }

    @Test
    void testFindByName_notFound() {
        when(authorRepository.findByNameIgnoreCase("NonExistent")).thenReturn(Optional.empty());

        Optional<Author> result = authorService.findByName("NonExistent");

        assertThat(result).isEmpty();
        verify(authorRepository, times(1)).findByNameIgnoreCase("NonExistent");
    }

    @Test
    void testFindEntityById_success() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));

        Author result = authorService.findEntityById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Author");
        verify(authorRepository, times(1)).findById(1L);
    }

    @Test
    void testFindEntityById_notFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.findEntityById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author not found");
    }

    @Test
    void testUpdateAuthor_success() {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setName("Updated Name");
        updateDto.setCountry("Poland");
        updateDto.setBirthYear(1975);

        Author updatedAuthor = Author.builder()
                .id(1L)
                .name("Updated Name")
                .country("Poland")
                .birthYear(1975)
                .build();

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.existsByNameIgnoreCase("Updated Name")).thenReturn(false);
        when(authorRepository.save(any(Author.class))).thenReturn(updatedAuthor);

        AuthorDto result = authorService.update(1L, updateDto);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getCountry()).isEqualTo("Poland");
        assertThat(result.getBirthYear()).isEqualTo(1975);
        verify(authorRepository, times(1)).findById(1L);
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void testUpdateAuthor_notFound() {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setName("Test");
        updateDto.setCountry("Ukraine");
        updateDto.setBirthYear(1950);

        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.update(99L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author not found");
    }

    @Test
    void testDeleteAuthor_success() {
        when(authorRepository.existsById(1L)).thenReturn(true);
        doNothing().when(authorRepository).deleteById(1L);
        authorService.delete(1L);
        verify(authorRepository, times(1)).deleteById(1L);
    }

    @Test
    void testCreateAuthor_duplicate() {
        AuthorDto dto = new AuthorDto();
        dto.setName("Duplicate Author");
        dto.setCountry("Ukraine");
        dto.setBirthYear(1950);

        when(authorRepository.existsByNameIgnoreCase("Duplicate Author")).thenReturn(true);

        assertThatThrownBy(() -> authorService.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Author with this name already exists");
    }

    @Test
    void testUpdateAuthor_duplicateName() {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setName("Existing Author");
        updateDto.setCountry("Ukraine");
        updateDto.setBirthYear(1975);

        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.existsByNameIgnoreCase("Existing Author")).thenReturn(true);

        assertThatThrownBy(() -> authorService.update(1L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Author with this name already exists");
    }
}