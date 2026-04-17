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
            // todo: should not update authors when some deleted or not exist
            // todo: check state of BD

        }
    }

    @Nested
    @DisplayName("Soft delete tests")
    class SoftDeleteTests {
        @Test
        void shouldMarkBookAsDeletedWhenExists() {
            Book book = saveTestBook();

            bookService.softDelete(book.getId());

            assertThat(book.isDeleted()).isTrue();
            assertThat(book.getDeletedAt()).isNotNull();
        }

        @Test
        void shouldNotDeleteAlreadyDeletedBook() {
            Book deletedBook = saveDeletedTestBook();
            Instant originalDeletedAt = deletedBook.getDeletedAt();

            bookService.softDelete(deletedBook.getId());

            assertThat(deletedBook.getDeletedAt()).isEqualTo(originalDeletedAt);
        }

        @Test
        void shouldCascadeSoftDeleteToRelatedEntitiesWhenConfigured() {
            Book book = saveTestBookWithReviews();

            bookService.softDelete(book.getId());

            assertThat(book.isDeleted()).isTrue();
            book.getReviews().forEach(review ->
                    assertThat(review.isDeleted()).isTrue()
            );
        }
    }

    @Nested
    @DisplayName("Search tests")
    class SearchTests {
        @Test
        void shouldFindBooksByTitleContaining() {
            Book book1 = saveBookWithTitle("Spring in Action");
            Book book2 = saveBookWithTitle("Spring Boot in Practice");
            Book book3 = saveBookWithTitle("Java Concurrency");

            Pageable pageable = PageRequest.of(0, 10);

            Page<BookResponse> result = bookService.searchByTitle("Spring", pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .extracting(BookResponse::getTitle)
                    .allMatch(title -> title.contains("Spring"));
        }

        @Test
        void shouldFindBooksByAuthorName() {
            Author author = saveTestAuthor();
            Book book1 = saveBookWithAuthor(author);
            Book book2 = saveAnotherBookWithAuthor(author);
            Book book3 = saveTestBook();

            Pageable pageable = PageRequest.of(0, 10);

            Page<BookResponse> result = bookService.findByAuthorName(author.getName(), pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .extracting(book -> book.getAuthor().getName())
                    .allMatch(name -> name.equals(author.getName()));
        }
    }

    @Nested
    @DisplayName("Batch operations tests")
    class BatchOperationsTests {
        @Test
        void shouldCreateMultipleBooksInBatch() {
            List<BookRequest> requests = List.of(
                    BookRequest.builder()
                            .title("Book 1")
                            .isbn("ISBN-001")
                            .genre(Genre.FICTION)
                            .price(BigDecimal.valueOf(19.99))
                            .publicationYear(Year.of(2020))
                            .authorId(1L)
                            .build(),
                    BookRequest.builder()
                            .title("Book 2")
                            .isbn("ISBN-002")
                            .genre(Genre.TECHNICAL)
                            .price(BigDecimal.valueOf(39.99))
                            .publicationYear(Year.of(2021))
                            .authorId(1L)
                            .build(),
                    BookRequest.builder()
                            .title("Book 3")
                            .isbn("ISBN-003")
                            .genre(Genre.SCIENCE)
                            .price(BigDecimal.valueOf(29.99))
                            .publicationYear(Year.of(2022))
                            .authorId(1L)
                            .build()
            );

            List<BookResponse> responses = bookService.createBatch(requests);

            assertThat(responses).hasSize(3);
            assertThat(responses)
                    .extracting(BookResponse::getTitle)
                    .containsExactly("Book 1", "Book 2", "Book 3");
        }

        @Test
        void shouldUpdatePricesInBatch() {
            Book book1 = saveTestBook();
            Book book2 = saveAnotherTestBook();

            List<Long> bookIds = List.of(book1.getId(), book2.getId());
            BigDecimal newPrice = BigDecimal.valueOf(49.99);

            bookService.updatePricesBatch(bookIds, newPrice);

            assertThat(book1.getPrice()).isEqualTo(newPrice);
            assertThat(book2.getPrice()).isEqualTo(newPrice);
        }
    }

    // Helper methods
    private BookResponse mapToResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .genre(book.getGenre())
                .price(book.getPrice())
                .publicationYear(book.getPublicationYear())
                .author(book.getAuthor() != null ? mapToAuthorResponse(book.getAuthor()) : null)
                .reviews(new ArrayList<>())
                .deletedAt(book.getDeletedAt())
                .build();
    }

    private AuthorResponse mapToAuthorResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .biography(author.getBiography())
                .deletedAt(author.getDeletedAt())
                .build();
    }
}
