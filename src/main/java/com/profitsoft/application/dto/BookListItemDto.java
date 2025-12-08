package com.profitsoft.application.dto;

import lombok.Data;

@Data
public class BookListItemDto {
    private Long id;
    private String title;
    private Integer yearPublished;
    private String authorName;
}
