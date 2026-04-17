package com.example.mvc2.services;

import com.example.mvc2.dtos.books.BookRequest;
import com.example.mvc2.dtos.books.BookResponse;
import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import com.example.mvc2.mappers.BookMapperImpl;
import com.example.mvc2.repositories.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.Year;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

@Import({BookService.class, BookMapperImpl.class})
public class BookServiceTest extends BaseServiceTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Nested
    @DisplayName("Create tests")
    class CreateTests {
        @Test
        void shouldReturnCorrectlyMappedDTOWhenProvidedCorrectRequest() {
            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Set<Long> authorIds = Set.of(author1.getId(), author2.getId());

            BookRequest request = BookRequest.builder()
                    .title("New Book")
                    .publicationYear(Year.of(2023))
                    .pageCount(150)
                    .isHardcover(false)
                    .authorIds(authorIds)
                    .build();

            BookResponse response = bookService.create(request);

            BookResponse expected = BookResponse.builder()
                    .title("New Book")
                    .publicationYear(Year.of(2023))
                    .pageCount(150)
                    .isHardcover(false)
                    .authorIds(authorIds)
                    .deletedAt(null)
                    .build();

            assertThat(response).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected);
        }

        @Test
        void shouldSaveAuthorsToDBWhenAuthorsNotDeleted() {
            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Set<Long> authorIds = Set.of(author1.getId(), author2.getId());

            BookRequest request = BookRequest.builder()
                    .title("New Book")
                    .publicationYear(Year.of(2023))
                    .pageCount(150)
                    .isHardcover(false)
                    .authorIds(authorIds)
                    .build();

            BookResponse response = bookService.create(request);

            Book saved = bookRepository.findById(response.getId()).orElseThrow();
            Set<Author> expected = Set.of(author1, author2);
            assertThat(saved.getAuthors()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Get one")
    class GetOneWithAuthorTests {
        @Test
        void shouldReturnBookWithAuthorsWhenBookIsNotDeleted() {
            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Book book = saveTestBook(Set.of(author1, author2));

            BookResponse response = bookService.getOne(book.getId());

            BookResponse expected = BookResponse.builder()
                    .title(book.getTitle())
                    .publicationYear(book.getPublicationYear())
                    .pageCount(book.getPageCount())
                    .isHardcover(book.getIsHardcover())
                    .authorIds(Set.of(author1.getId(), author2.getId()))
                    .deletedAt(null)
                    .build();

            assertThat(response).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Get list with pagination and filtering")
    class GetListWithPaginationAndFilteringTests {
        @Test
        void shouldReturnNotDeletedBooksWithPaginationWhenRequestIsCorrect() {
            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Book book1 = saveTestBook(Set.of(author1));
            Book book2 = saveAnotherTestBook();
            Book deletedBook = saveDeletedTestBook();

            Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());

            Page<BookResponse> result = bookService.getListWithPagination(pageable);

            List<BookResponse> expectedContent = List.of(
                    mapToResponse(book1),
                    mapToResponse(book2)
            );

            assertThat(result.getContent())
                    .usingRecursiveComparison()
                    .ignoringCollectionOrder()
                    .isEqualTo(expectedContent);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getNumber()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
        }

        @Test
        void shouldReturnFilteredBooksByGenreWhenGenreProvided() {
            Book technicalBook = saveTechnicalBook();
            Book fictionBook = saveFictionBook();

            Pageable pageable = PageRequest.of(0, 10);

            Page<BookResponse> result = bookService.getListByGenre(Genre.TECHNICAL, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getGenre()).isEqualTo(Genre.TECHNICAL);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void shouldReturnFilteredBooksByPriceRangeWhenRangeProvided() {
            Book cheapBook = saveBookWithPrice(BigDecimal.valueOf(10));
            Book mediumBook = saveBookWithPrice(BigDecimal.valueOf(30));
            Book expensiveBook = saveBookWithPrice(BigDecimal.valueOf(60));

            Pageable pageable = PageRequest.of(0, 10);

            Page<BookResponse> result = bookService.getListByPriceRange(
                    BigDecimal.valueOf(20),
                    BigDecimal.valueOf(50),
                    pageable
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(30));
        }
    }

    @Nested
    @DisplayName("Update tests")
    class UpdateTests {
        @Test
        void shouldReturnUpdatedBookWhenNotDeleted() {
            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Set<Author> authors = Set.of(author1, author2);
            Book book = saveTestBook(authors);

            Set<Long> authorIds = Set.of(author2.getId());

            BookRequest request = BookRequest.builder()
                    .title("Updated Title")
                    .publicationYear(Year.of(2024))
                    .pageCount(300)
                    .isHardcover(false)
                    .authorIds(authorIds)
                    .build();

            BookResponse response = bookService.update(book.getId(), request);

            BookResponse expected = new BookResponse(
                    book.getId(),
                    request.getTitle(),
                    request.getPublicationYear(),
                    request.getPageCount(),
                    request.getIsHardcover(),
                    request.getAuthorIds(),
                    null
            );

            assertThat(response).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        void shouldUpdateBookAuthorsWhenNewAuthorIdsValid() {
            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Author author3 = saveTestAuthor3();
            Set<Author> initialAuthors = Set.of(author1, author2);
            Book book = saveTestBook(initialAuthors);

            Set<Long> newAuthorIds = Set.of(author2.getId(), author3.getId());
            Set<Author> newAuthors  = Set.of(author2, author3);

            BookRequest request = BookRequest.builder()
                    .title(book.getTitle())
                    .publicationYear(book.getPublicationYear())
                    .pageCount(book.getPageCount())
                    .isHardcover(book.getIsHardcover())
                    .authorIds(newAuthorIds)
                    .build();

            BookResponse response = bookService.update(book.getId(), request);
            Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();

            BookResponse expected = new BookResponse(
                    book.getId(),
                    request.getTitle(),
                    request.getPublicationYear(),
                    request.getPageCount(),
                    request.getIsHardcover(),
                    newAuthorIds,
                    null
            );

            assertThat(response).usingRecursiveComparison().isEqualTo(expected);
            assertThat(updatedBook.getAuthors()).isEqualTo(newAuthors);
        }

        @Test
        void shouldNotUpdateBookWhenDeleted() {
            Author author1 = saveTestAuthor();
            Author deletedAuthor = saveDeletedTestAuthor();
            Book book = saveTestBook(Set.of(author1));

            Set<Long> newAuthorIds = Set.of(author1.getId(), deletedAuthor.getId());

            BookRequest request = BookRequest.builder()
                    .title(book.getTitle())
                    .publicationYear(book.getPublicationYear())
                    .pageCount(book.getPageCount())
                    .isHardcover(book.getIsHardcover())
                    .authorIds(newAuthorIds)
                    .build();

            try {
                bookService.update(book.getId(), request);
            } catch (RuntimeException ignored) {
            }

            Book notUpdatedBook = bookRepository.findById(book.getId()).orElseThrow();
            assertThat(notUpdatedBook).usingRecursiveComparison().isEqualTo(book);
        }
    }

    @Nested
    @DisplayName("Soft delete tests")
    class SoftDeleteTests {
        @Test
        void shouldMarkBookAsDeletedWhenExists() {
            Author author = saveTestAuthor();
            Book book = saveTestBook(Set.of(author));

            bookService.deleteSoft(book.getId());

            assertThat(book.isDeleted()).isTrue();
            assertThat(book.getDeletedAt()).isNotNull();
        }

        @Test
        void shouldNotDeleteAlreadyDeletedBook() {
            Author author = saveTestAuthor();
            Book deletedBook = saveDeletedTestBook(Set.of(author));
            Instant originalDeletedAt = deletedBook.getDeletedAt();

            try {
                bookService.deleteSoft(deletedBook.getId());
            } catch (RuntimeException ignored) {
            }

            assertThat(deletedBook.getDeletedAt()).isEqualTo(originalDeletedAt);
        }
    }
}
