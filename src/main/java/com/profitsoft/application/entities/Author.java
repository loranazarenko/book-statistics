package com.profitsoft.application.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Author entity representing a book author.
 * Secondary entity in Book-Author relationship (one-to-many from Author's perspective).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Author {

    @JsonProperty("name")
    private String name;

    @JsonProperty("country")
    private String country;

    @JsonProperty("birth_year")
    private Integer birthYear;

    /**
     * Constructor for simple author name (backward compatibility)
     */
    public Author(String name) {
        this.name = name;
    }
}
