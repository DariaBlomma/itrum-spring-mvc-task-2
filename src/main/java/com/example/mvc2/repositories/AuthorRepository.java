package com.example.mvc2.repositories;

import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AuthorRepository extends JpaRepository<Book, Long> {
    @Query("SELECT a FROM Author a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Author> findActiveById(@Param("id") Long id);

    @Query("SELECT a FROM Author a WHERE a.id IN :ids AND a.deletedAt IS NULL")
    List<Author> findActiveByIds(@Param("ids") Set<Long> ids);
}
