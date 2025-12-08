package com.profitsoft.application.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Author entity representing a book author.
 * Secondary entity in Book-Author relationship
 */
@Entity
@Table(name = "author",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
@RequiredArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(exclude = "id")
@ToString(exclude = "id")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("name")
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @JsonProperty("country")
    @Column(length = 200)
    private String country;

    @JsonProperty("birth_year")
    @Column(name = "birth_year")
    @Min(value = 1, message = "Birth year must be positive")
    @Max(value = 2025, message = "Birth year cannot be in the future")
    private Integer birthYear;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public Author(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name.trim();
    }

    public Author(String name, String country, Integer birthYear) {
        this(name);
        this.country = country;
        this.birthYear = birthYear;
    }
}
