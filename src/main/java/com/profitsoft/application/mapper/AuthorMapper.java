package com.profitsoft.application.mapper;

import com.profitsoft.application.dto.AuthorDto;
import com.profitsoft.application.entities.Author;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Author entity and AuthorDto conversion
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorMapper {

    default AuthorDto toDto(Author author) {
        if (author == null) {
            return null;
        }

        AuthorDto dto = new AuthorDto();
        dto.setId(author.getId());
        dto.setName(author.getName());
        dto.setCountry(author.getCountry());
        dto.setBirthYear(author.getBirthYear());
        return dto;
    }

    default Author toEntity(AuthorDto authorDto) {
        if (authorDto == null) {
            return null;
        }

        return Author.builder()
                .id(authorDto.getId())
                .name(authorDto.getName())
                .country(authorDto.getCountry())
                .birthYear(authorDto.getBirthYear())
                .build();
    }
}
