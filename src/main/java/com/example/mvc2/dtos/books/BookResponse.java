package com.example.mvc2.dtos.books;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.time.Year;
import java.util.Set;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private Long id;
    private String title;
    private Year publicationYear;
    private Integer pageCount;
    private Boolean isHardcover;
    private Set<Long> authorIds;
    private Instant deletedAt;
}
