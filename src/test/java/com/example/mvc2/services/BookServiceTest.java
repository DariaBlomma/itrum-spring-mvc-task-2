package com.example.mvc2.services;

import com.example.mvc2.dtos.books.BookRequest;
import com.example.mvc2.dtos.books.BookResponse;
import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import com.example.mvc2.mappers.BookMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Import({BookService.class, BookMapperImpl.class})
public class BookServiceTest extends BaseServiceTest {
    @Autowired
    private BookService bookService;

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
    class GetOneTests {
        @Test
        void shouldReturnBookWithAuthorsWhenBookIsNotDeleted() {
            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Book book = saveTestBook(Set.of(author1, author2));

            BookResponse response = bookService.getOne(book.getId());

            BookResponse expected = BookResponse.builder()
                    .id(book.getId())
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
    @DisplayName("Get list")
    class GetListTests {
        @Test
        void shouldReturnEmptyPageWhenNoBooksExist() {
            Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());
            Page<BookResponse> result = bookService.getList(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getNumber()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
        }

        @Test
        void shouldReturnCorrectTotalCountWhenMultipleAuthorsPerBook() {
            save15BooksForPaginationAndCountTest();

            long expectedTotalCount = 15;
            Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());

            Page<BookResponse> result = bookService.getList(pageable);

            assertThat(result.getTotalElements()).isEqualTo(expectedTotalCount);
            assertThat(result.getTotalPages()).isEqualTo(2); // 15 элементов / 10 = 2 страницы
        }

        @Test
        void shouldReturnDistinctBooks_WhenBookHasMultipleAuthors() {
            String bookName = "Book With 3 Authors";
            saveBookWith3Authors(bookName); // создает 3 строки в JOIN

            Page<BookResponse> result = bookService.getList(Pageable.ofSize(10));

            long count = result.getContent().stream()
                    .filter(b -> b.getTitle().equals(bookName))
                    .count();
            assertEquals(1, count, "Book should appear only once despite having 3 authors");
        }

        @Test
        void shouldExcludeDeletedAuthorsFromResponse() {
            Author activeAuthor = saveTestAuthor();
            Author deletedAuthor = saveDeletedTestAuthor();
            String bookName = "Book With Mixed Authors";
            Book book = getBaseBook().toBuilder()
                    .title(bookName)
                    .authors(Set.of(activeAuthor, deletedAuthor))
                    .build();
            bookRepository.save(book);

            Page<BookResponse> result = bookService.getList(Pageable.ofSize(10));
            BookResponse resultBook = result.getContent().stream()
                    .filter(b -> b.getTitle().equals(bookName))
                            .findFirst()
                    .orElseThrow(() -> new AssertionError("Book not found in response"));

            assertThat(resultBook.getAuthorIds()).isEqualTo(Set.of(activeAuthor.getId()));
        }

        @Test
        void shouldExcludeDeletedBookFromResponse() {
            Author activeAuthor = saveTestAuthor();
            Author anotherAuthor = saveAnotherTestAuthor();

           saveTestBook(Set.of(activeAuthor));
           saveDeletedTestBook(Set.of(anotherAuthor, activeAuthor));

            Page<BookResponse> result = bookService.getList(Pageable.ofSize(10));

            boolean hasDeletedBook = result.getContent().stream()
                    .anyMatch(b -> b.getDeletedAt() != null);
            assertFalse(hasDeletedBook, "Deleted books should be excluded");
        }

        @Test
        void shouldApplyFallbackSort_WhenBookNamesAreIdentical() {
            Author sharedAuthor = saveTestAuthor();

            Book book = Book.builder()
                    .title("First Book")
                    .publicationYear(Year.of(2024))
                    .pageCount(100)
                    .isHardcover(true)
                    .authors(Set.of(sharedAuthor))
                    .deletedAt(null)
                    .build();
            Book book2 = book.toBuilder().build();
            Book savedBook1 = bookRepository.save(book);
            Book savedBook2 = bookRepository.save(book2);

            assertTrue(savedBook1.getId() < savedBook2.getId(),
                    "Test setup: book1 should have smaller ID than book2");

            Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());
            Page<BookResponse> result = bookService.getList(pageable);

            assertEquals(2, result.getContent().size());

            // 2. Порядок по ID: book1 (меньший ID) должен идти первым
            // Это доказывает, что fallback-сортировка работает
            List<Long> actualIds = result.getContent().stream()
                    .map(BookResponse::getId)
                    .toList();

            assertEquals(List.of(savedBook1.getId(), savedBook2.getId()), actualIds,
                    "Books with same title should be sorted by ID (fallback)");
        }

        /**
         * Создает набор книг для тестирования пагинации и COUNT(DISTINCT).
         * Возвращает список созданных книг.
         * Структура:
         * - 5 книг с 1 автором
         * - 5 книг с 2 авторами (создают дубли строк при JOIN)
         * - 5 книг с 3 авторами (создают еще больше дублей)
         * Итого: 15 книг, но > 15 строк в результирующем сете JOIN.
         */
        private List<Book> save15BooksForPaginationAndCountTest() {
            List<Book> createdBooks = new ArrayList<>();

            Author author1 = saveTestAuthor();
            Author author2 = saveAnotherTestAuthor();
            Author author3 = saveTestAuthor3();

            Set<Author> singleAuthorSet = Set.of(author1);
            Set<Author> twoAuthorsSet = Set.of(author1, author2);
            Set<Author> threeAuthorsSet = Set.of(author1, author2, author3);

            // Создаем 5 книг с 1 автором
            for (int i = 0; i < 5; i++) {
                createdBooks.add(saveTestBook(singleAuthorSet));
            }

            // Создаем 5 книг с 2 авторами
            for (int i = 0; i < 5; i++) {
                createdBooks.add(saveTestBook(twoAuthorsSet));
            }

            // Создаем 5 книг с 3 авторами
            for (int i = 0; i < 5; i++) {
                createdBooks.add(saveTestBook(threeAuthorsSet));
            }

            return createdBooks;
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
