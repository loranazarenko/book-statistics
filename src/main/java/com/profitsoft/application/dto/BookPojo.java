package com.profitsoft.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookPojo {

    @JsonProperty("title")
    private String title;

    @JsonProperty("yearPublished")
    private Integer yearPublished;

    @JsonProperty("year_published")
    private Integer year_published;

    @JsonProperty("genre")
    @JsonAlias("genres")
    private List<String> genres = new ArrayList<>();

    @JsonProperty("authorId")
    private Long authorId;

    @JsonProperty("author")
    private Object author;

    public Long getAuthorIdValue() {
        if (authorId != null) {
            return authorId;
        }

        if (author instanceof Map) {
            Object id = ((Map<String, ?>) author).get("id");
            if (id instanceof Number) {
                return ((Number) id).longValue();
            }
        }
        return null;
    }

    public String getAuthorName() {
        if (author == null) return null;
        if (author instanceof Map) {
            Object name = ((Map<?, ?>) author).get("name");
            return name != null ? name.toString() : null;
        }
        return author.toString();
    }
}