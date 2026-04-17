package com.example.mvc2.repositories;

import com.example.mvc2.entities.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Book> findActiveById(@Param("id") Long id);

    @Query("SELECT b FROM Book b WHERE b.id IN :ids AND b.deletedAt IS NULL")
    List<Book> finaActiveByIds(@Param("ids")Set<Long> ids);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.authors a WHERE b.id = :id AND b.deletedAt IS NULL AND (a.deletedAt IS NULL OR a IS NULL)")
    Optional<Book> findActiveByIdWithAuthors(@Param("id") Long id);

    @Query(
            value = "SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.authors a WHERE b.deletedAt IS NULL AND (a.deletedAt IS NULL OR a IS NULL)",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Book b LEFT JOIN b.authors a WHERE b.deletedAt IS NULL AND (a.deletedAt IS NULL OR a IS NULL)"
    )
    Page<Book> findAllActiveWithAuthorsPaginated(Pageable pageable);
}
