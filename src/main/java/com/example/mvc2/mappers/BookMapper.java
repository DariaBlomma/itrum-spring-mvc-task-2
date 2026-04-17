package com.example.mvc2.mappers;

import com.example.mvc2.dtos.books.BookRequest;
import com.example.mvc2.dtos.books.BookResponse;
import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookMapper {
    Book toEntity(BookRequest request);

    @Mapping(source = "authors", target = "authorIds")
    BookResponse toResponse(Book book);

    default Set<Long> mapAuthorsToIds(Collection<Author> authors) {
        if (authors == null) return Set.of();
        return authors.stream().map(Author::getId).collect(Collectors.toUnmodifiableSet());
    }

    void update(BookRequest request, @MappingTarget Book user);
}
