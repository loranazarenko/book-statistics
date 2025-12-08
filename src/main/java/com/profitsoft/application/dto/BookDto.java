package com.profitsoft.application.dto;

import java.util.List;

import lombok.Data;

@Data
public class BookDto {
    private Long id;
    private String title;
    private Integer yearPublished;
    private List<String> genres;
    private AuthorDto author;
}
