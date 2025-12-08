package com.profitsoft.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookListRequest {

    @Min(value = 0, message = "Page must be >= 0")
    private int page = 0;

    @Min(value = 1, message = "Size must be >= 1")
    @Max(value = 100, message = "Size must be <= 100")
    private int size = 10;

    private String title;
    private Long authorId;
    private String genre;

    @Pattern(regexp = "^(id|title|yearPublished|authorId)$",
            message = "sortBy must be one of: id, title, yearPublished, authorId")
    private String sortBy = "id";

    @Pattern(regexp = "^(ASC|DESC)$", message = "sortOrder must be ASC or DESC")
    private String sortOrder = "DESC";
}