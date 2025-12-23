package com.profitsoft.application.mapper;

import com.profitsoft.application.dto.BookDto;
import com.profitsoft.application.dto.BookListItemDto;
import com.profitsoft.application.entities.Book;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Book entity and BookDto conversion
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AuthorMapper.class)
public interface BookMapper {

    BookDto toDto(Book book);

    default BookListItemDto toListItemDto(Book book) {
        if (book == null) {
            return null;
        }

        BookListItemDto dto = new BookListItemDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setYearPublished(book.getYearPublished());
        if (book.getAuthor() != null) {
            dto.setAuthorName(book.getAuthor().getName());
        }
        return dto;
    }

    Book toEntity(BookDto bookDto);
}
