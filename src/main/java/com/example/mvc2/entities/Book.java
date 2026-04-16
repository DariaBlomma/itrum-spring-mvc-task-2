package com.example.mvc2.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.time.Year;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "publication_year")
    private Year publicationYear;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "is_hardcover")
    private Boolean isHardcover;

    @ManyToMany
    @JoinTable(
            name = "book_author",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();

    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
