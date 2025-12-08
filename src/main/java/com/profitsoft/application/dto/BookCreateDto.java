package com.profitsoft.application.dto;

import jakarta.validation.constraints.*;

import java.util.List;

import lombok.Data;

@Data
public class BookCreateDto {
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotNull(message = "Author ID cannot be null")
    private Long authorId;

    @NotNull(message = "Year published cannot be null")
    @Min(value = 1, message = "Year published must be positive")
    private Integer yearPublished;

    private List<String> genres;
}
