package com.example.mvc2.mappers;

import com.example.mvc2.dtos.authors.AuthorRequest;
import com.example.mvc2.dtos.authors.AuthorResponse;
import com.example.mvc2.entities.Author;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    Author toEntity(AuthorRequest request);

    AuthorResponse toResponse(Author author);;
}
