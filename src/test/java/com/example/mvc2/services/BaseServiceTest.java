package com.example.mvc2.services;

import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import com.example.mvc2.repositories.AuthorRepository;
import com.example.mvc2.repositories.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import java.time.Instant;
import java.time.Year;
import java.util.Set;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public abstract class BaseServiceTest {
    @Autowired
    protected BookRepository bookRepository;

    @Autowired
    protected AuthorRepository authorRepository;

    protected Author saveTestAuthor() {
        Author author = Author.builder()
                .name("Author One")
                .build();
        return authorRepository.save(author);
    }

    protected Author saveAnotherTestAuthor() {
        Author author = Author.builder()
                .name("Author Two")
                .build();
        return authorRepository.save(author);
    }

    protected Author saveTestAuthor3() {
        Author author = Author.builder()
                .name("Author Three")
                .build();
        return authorRepository.save(author);
    }

    protected Author saveDeletedTestAuthor() {
        Author author = Author.builder()
                .name("Deleted Author")
                .deletedAt(Instant.now())
                .build();
        return authorRepository.save(author);
    }

    protected Book saveTestBook(Set<Author> authors) {
        Book book = Book.builder()
                .title("Test Book")
                .publicationYear(Year.of(2020))
                .pageCount(100)
                .isHardcover(true)
                .authors(authors)
                .build();
        return bookRepository.save(book);
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
        return bookRepository.save(book);
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
        return bookRepository.save(book);
    }

    protected Book getBaseBook() {
        return Book.builder()
                .publicationYear(Year.now())
                .pageCount(34)
                .isHardcover(true)
                .authors(Set.of())
                .deletedAt(null)
                .build();
    }

    protected Book saveBookWith3Authors(String bookName) {
        Author activeAuthor = saveTestAuthor();
        Author a1 = authorRepository.save(activeAuthor.toBuilder().id(null).name("A1").build());
        Author a2 = authorRepository.save(activeAuthor.toBuilder().id(null).name("A2").build());
        Author a3 = authorRepository.save(activeAuthor.toBuilder().id(null).name("A3").build());
        Book book = getBaseBook().toBuilder()
                .title(bookName)
                .authors(Set.of(a1, a2, a3))
                .build();
        return bookRepository.save(book);
    }
}