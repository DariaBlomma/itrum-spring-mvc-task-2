package com.example.mvc2.services;

import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import java.time.Instant;
import java.time.Year;
import java.util.Set;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public abstract class BaseServiceTest {

    @Autowired
    protected TestEntityManager entityManager;

    protected Author saveTestAuthor() {
        Author author = Author.builder()
                .name("Author One")
                .build();
        entityManager.persistAndFlush(author);
        return author;
    }

    protected Author saveAnotherTestAuthor() {
        Author author = Author.builder()
                .name("Author Two")
                .build();
        entityManager.persistAndFlush(author);
        return author;
    }

    protected Author saveTestAuthor3() {
        Author author = Author.builder()
                .name("Author Three")
                .build();
        entityManager.persistAndFlush(author);
        return author;
    }

    protected Author saveDeletedTestAuthor() {
        Author author = Author.builder()
                .name("Deleted Author")
                .deletedAt(Instant.now())
                .build();
        entityManager.persistAndFlush(author);
        return author;
    }

    protected Book saveTestBook(Set<Author> authors) {
        Book book = Book.builder()
                .title("Test Book")
                .publicationYear(Year.of(2020))
                .pageCount(100)
                .isHardcover(true)
                .authors(authors)
                .build();
        entityManager.persistAndFlush(book);
        return book;
    }

    protected Book saveAnotherTestBook(Set<Author> authors) {
        Book book = Book.builder()
                .title("Another Book")
                .publicationYear(Year.of(2019))
                .pageCount(200)
                .isHardcover(false)
                .authors(authors)
                .deletedAt(null)
                .build();
        entityManager.persistAndFlush(book);
        return book;
    }

    protected Book saveDeletedTestBook(Set<Author> authors) {
        Book book = Book.builder()
                .title("Deleted Book")
                .publicationYear(Year.of(2019))
                .pageCount(200)
                .isHardcover(false)
                .authors(authors)
                .deletedAt(Instant.now())
                .build();
        entityManager.persistAndFlush(book);
        return book;
    }
}