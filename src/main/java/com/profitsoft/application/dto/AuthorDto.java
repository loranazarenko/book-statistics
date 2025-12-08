package com.profitsoft.application.dto;

import lombok.Data;

@Data
public class AuthorDto {
    private Long id;
    private String name;
    private String country;
    private Integer birthYear;
}
