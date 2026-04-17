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
        void shouldReturnDistinctBooksWithActiveAuthorsOnly() {
            Author activeAuthor = saveTestAuthor();
            Author deletedAuthor = saveDeletedTestAuthor();

            Book baseBook = Book.builder()
                    .publicationYear(Year.now())
                    .pageCount(34)
                    .isHardcover(true)
                    .authors(Set.of())
                    .deletedAt(null)
                    .build();

            // Книга с 2 активными + 1 удаленным автором (проверка фильтрации авторов)
            Book book1 = baseBook.toBuilder()
                    .title("Book With Mixed Authors")
                    .authors(Set.of(activeAuthor, deletedAuthor))
                    .build();
            bookRepository.save(book1);

            // Книга с 3 активными авторами (проверка на дубликаты DISTINCT)
            Author a1 = authorRepository.save(activeAuthor.toBuilder().name("A1").build());
            Author a2 = authorRepository.save(activeAuthor.toBuilder().name("A2").build());
            Author a3 = authorRepository.save(activeAuthor.toBuilder().name("A3").build());
            Book book2 = baseBook.toBuilder()
                    .title("Book With 3 Authors")
                    .authors(Set.of(a1, a2, a3))
                    .build();
            bookRepository.save(book2);

            // Удаленная книга (проверка фильтра книг)
           saveDeletedTestBook(Set.of(activeAuthor));

            Page<Book> result = bookRepository.findAllActiveWithAuthorsPaginated(Pageable.ofSize(10));


            // 1. Проверка DISTINCT: book2 не должен дублироваться, несмотря на 3 авторов
            assertEquals(3, result.getContent().size(), "Should have 3 active books");

            long book2Count = result.getContent().stream()
                    .filter(b -> b.getTitle().equals("Book With 3 Authors"))
                    .count();
            assertEquals(1, book2Count, "Book with 3 authors should appear ONLY ONCE (DISTINCT works)");

            // 2. Проверка JOIN FETCH: доступ к авторам не должен вызывать исключений
            // и коллекция не должна быть пустой для book1 и book2
            Book fetchedBook1 = result.getContent().stream()
                    .filter(b -> b.getTitle().equals("Book With Mixed Authors"))
                    .findFirst().orElseThrow(() -> new AssertionError("Expected book not found"));

            // 3. Проверка фильтра авторов: удаленный автор не должен попасть в коллекцию
            assertEquals(1, fetchedBook1.getAuthors().size(), "Deleted author should be filtered out");
            boolean hasDeletedAuthor = fetchedBook1.getAuthors().stream()
                    .anyMatch(author -> author.getDeletedAt() == null);
            assertFalse(hasDeletedAuthor, "Deleted author should not be in the collection");

            // 4. Проверка фильтра книг: удаленная книга не должна быть в списке
            boolean hasDeletedBook = result.getContent().stream()
                    .anyMatch(b -> b.getDeletedAt() == null);
            assertFalse(hasDeletedBook, "Deleted books should be excluded");
        }


        @Test
        void shouldFilterByIsHardcoverWithNullValuesAtEnd() {
            List<Book> booksForTest = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Book book = createBookTemplate("Title " + i);
                book.setHardCover(true);
                entityManager.persist(book);
                booksForTest.add(book);
            }
            for (int i = 0; i < 5; i++) {
                Book book = createBookTemplate("Title Null " + i);
                book.setHardCover(null);
                entityManager.persist(book);
                booksForTest.add(book);
            }
            for (int i = 0; i < 5; i++) {
                Book book = createBookTemplate("Title False " + i);
                book.setHardCover(false);
                entityManager.persist(book);
                booksForTest.add(book);
            }
            entityManager.flush();

            Pageable pageable = PageRequest.of(0, 999, Sort.by(Sort.Direction.DESC, "hardCover"));
            Page<BookDto> result = bookService.getList(pageable);

            List<BookDto> content = result.getContent();
            assertThat(content).hasSize(15);

            for (int i = 0; i < 5; i++) {
                assertThat(content.get(i).isHardCover()).isTrue();
            }
            for (int i = 5; i < 10; i++) {
                assertThat(content.get(i).isHardCover()).isFalse();
            }
            for (int i = 10; i < 15; i++) {
                assertThat(content.get(i).isHardCover()).isNull();
            }
        }

        @Test
        void shouldSortByPublicationYearWithFallbackToId() {
            Author author = createAuthor("Common", "Author");
            entityManager.persist(author);
            entityManager.flush();

            Book bookSameYear1 = createBookTemplate("Same Year 1");
            bookSameYear1.setPublicationYear(2020);
            bookSameYear1.setAuthors(List.of(author));
            entityManager.persist(bookSameYear1);

            Book bookSameYear2 = createBookTemplate("Same Year 2");
            bookSameYear2.setPublicationYear(2020);
            bookSameYear2.setAuthors(List.of(author));
            entityManager.persist(bookSameYear2);

            Book bookLaterYear = createBookTemplate("Later Year");
            bookLaterYear.setPublicationYear(2022);
            bookLaterYear.setAuthors(List.of(author));
            entityManager.persist(bookLaterYear);

            Book bookEarlierYear = createBookTemplate("Earlier Year");
            bookEarlierYear.setPublicationYear(2018);
            bookEarlierYear.setAuthors(List.of(author));
            entityManager.persist(bookEarlierYear);

            entityManager.flush();

            Pageable pageable = PageRequest.of(0, 999, Sort.by(Sort.Direction.ASC, "publicationYear", "id"));
            Page<BookDto> result = bookService.getList(pageable);

            List<BookDto> content = result.getContent();
            assertThat(content).hasSize(4);

            assertThat(content.get(0).getId()).isEqualTo(bookEarlierYear.getId());
            assertThat(content.get(1).getId()).isEqualTo(bookSameYear1.getId());
            assertThat(content.get(2).getId()).isEqualTo(bookSameYear2.getId());
            assertThat(content.get(3).getId()).isEqualTo(bookLaterYear.getId());
        }

        @Test
        void shouldSortByPageCountAscending() {
            Author author = createAuthor("Page", "Counter");
            entityManager.persist(author);
            entityManager.flush();

            Book bookLowPages = createBookTemplate("Low Pages");
            bookLowPages.setPagesCount(100);
            bookLowPages.setAuthors(List.of(author));
            entityManager.persist(bookLowPages);

            Book bookHighPages = createBookTemplate("High Pages");
            bookHighPages.setPagesCount(500);
            bookHighPages.setAuthors(List.of(author));
            entityManager.persist(bookHighPages);

            Book bookMediumPages = createBookTemplate("Medium Pages");
            bookMediumPages.setPagesCount(250);
            bookMediumPages.setAuthors(List.of(author));
            entityManager.persist(bookMediumPages);

            entityManager.flush();

            Pageable pageable = PageRequest.of(0, 999, Sort.by(Sort.Direction.ASC, "pagesCount"));
            Page<BookDto> result = bookService.getList(pageable);

            List<BookDto> content = result.getContent();
            assertThat(content).hasSize(3);

            assertThat(content.get(0).getId()).isEqualTo(bookLowPages.getId());
            assertThat(content.get(1).getId()).isEqualTo(bookMediumPages.getId());
            assertThat(content.get(2).getId()).isEqualTo(bookHighPages.getId());
        }

        @Test
        void shouldSortByAuthorsNameWithFallbackToId() {
            Author authorZ = createAuthor("Zoe", "Author");
            Author authorA = createAuthor("Alice", "Author");
            Author authorM = createAuthor("Michael", "Author");

            entityManager.persist(authorZ);
            entityManager.persist(authorA);
            entityManager.persist(authorM);
            entityManager.flush();

            Book bookByZ = createBookTemplate("Book By Z");
            bookByZ.setAuthors(List.of(authorZ));
            entityManager.persist(bookByZ);

            Book bookByA = createBookTemplate("Book By A");
            bookByA.setAuthors(List.of(authorA));
            entityManager.persist(bookByA);

            Book bookByM = createBookTemplate("Book By M");
            bookByM.setAuthors(List.of(authorM));
            entityManager.persist(bookByM);

            Book bookBySameAuthorWithHigherId = createBookTemplate("Another Book By A");
            bookBySameAuthorWithHigherId.setAuthors(List.of(authorA));
            entityManager.persist(bookBySameAuthorWithHigherId);

            entityManager.flush();

            Pageable pageable = PageRequest.of(0, 999, Sort.by(Sort.Direction.ASC, "authors.name"));
            Page<BookDto> result = bookService.getList(pageable);

            List<BookDto> content = result.getContent();
            assertThat(content).hasSize(4);

            assertThat(content.get(0).getId()).isEqualTo(bookByA.getId());
            assertThat(content.get(1).getId()).isEqualTo(bookBySameAuthorWithHigherId.getId());
            assertThat(content.get(2).getId()).isEqualTo(bookByM.getId());
            assertThat(content.get(3).getId()).isEqualTo(bookByZ.getId());
        }

        @Test
        void shouldExcludeDeletedBooksFromResults() {
            Book bookToDelete = getBookById(1L);
            bookToDelete.setStatus(BookStatus.DELETED);
            entityManager.persistAndFlush(bookToDelete);

            Pageable pageable = PageRequest.of(0, 999, Sort.unsorted());
            Page<BookDto> result = bookService.getList(pageable);

            assertThat(result.getTotalElements()).isEqualTo(24);
            assertThat(result.getContent())
                    .extracting(BookDto::getId)
                    .doesNotContain(bookToDelete.getId());
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
