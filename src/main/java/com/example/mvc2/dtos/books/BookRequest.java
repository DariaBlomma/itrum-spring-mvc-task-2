package com.example.mvc2.dtos.books;

import com.example.mvc2.dtos.authors.AuthorRequest;
import com.example.mvc2.entities.Author;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Year;
import java.util.Set;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class BookRequest {
    @NotNull(message = "Title is required")
    @Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    private String title;

    @NotNull(message = "Publication year is required")
    @PastOrPresent(message = "Publication year cannot be in the future")
    private Year publicationYear;

    @Min(value = 1, message = "Page count must be at least 1")
    private Integer pageCount;

    private Boolean isHardcover;

    @NotNull(message = "Authors list is required")
    @NotEmpty(message = "At least one author is required")
    private Set<Long> authorIds;
}
