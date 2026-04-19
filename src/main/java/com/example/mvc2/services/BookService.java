package com.example.mvc2.services;

import com.example.mvc2.dtos.books.BookRequest;
import com.example.mvc2.dtos.books.BookResponse;
import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import com.example.mvc2.exceptions.ConflictException;
import com.example.mvc2.exceptions.InvalidRequestException;
import com.example.mvc2.exceptions.ResourceNotFoundException;
import com.example.mvc2.mappers.BookMapper;
import com.example.mvc2.repositories.AuthorRepository;
import com.example.mvc2.repositories.BookRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final AuthorRepository authorRepository;

    private final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "title",
            "publicationYear",
            "pageCount",
            "isHardcover"
    );

    @Transactional
    public BookResponse create(BookRequest request) {
        Book book = bookMapper.toEntity(request);
        List<Author> activeAuthors = authorRepository.findActiveByIds(request.getAuthorIds());
        checkAuthorsOfRequest(activeAuthors, request.getAuthorIds());
        book.setAuthors(new HashSet<>(activeAuthors));
        Book saved = bookRepository.save(book);
        return bookMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BookResponse getOne(Long bookId) {
        Book book = bookRepository.findActiveByIdWithAuthors(bookId).orElseThrow(
                () -> new ResourceNotFoundException("Book does not exist or deleted with such id " + bookId));
        return bookMapper.toResponse(book);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> getList(Pageable pageable) {
        validateSortFields(pageable.getSort());
        Pageable stablePageable = addFallbackSort(pageable);
        Page<Book> bookPage = bookRepository.findAllActiveWithAuthorsPaginated(stablePageable);
        return bookPage.map(bookMapper::toResponse);
    }

    @Transactional
    public BookResponse update(Long bookId, BookRequest request) {
        Book book = bookRepository.findActiveByIdWithAuthors(bookId).orElseThrow(
                () -> new ResourceNotFoundException("Book does not exist or deleted with such id " + bookId));
        Set<Long> requestIds = request.getAuthorIds();
        List<Author> activeAuthors = authorRepository.findActiveByIds(requestIds);
        checkAuthorsOfRequest(activeAuthors, request.getAuthorIds());
        bookMapper.update(request, book);
        book.setAuthors(new HashSet<>(activeAuthors));
        return bookMapper.toResponse(book);
    }

    @Transactional
    public void deleteSoft(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(
                () -> new ResourceNotFoundException("Book does not exist such ids " + bookId));
        if (book.isDeleted()) {
            throw new ConflictException("Book is already deleted");
        }
        book.setDeletedAt(Instant.now());
    }

    private void checkAuthorsOfRequest(List<Author> activeAuthors, Set<Long> requestIds) {
        if (activeAuthors.size() < requestIds.size()) {
            Set<Long> foundIds = activeAuthors.stream()
                    .map(Author::getId)
                    .collect(Collectors.toSet());

            Set<Long> invalidIds = requestIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new InvalidRequestException("Some authors do not exist or were deleted. Invalid ids are: " + invalidIds);
        }
    }

    private void validateSortFields(Sort sort) {
        for (Sort.Order order : sort) {
            String prop = order.getProperty();
            if (!ALLOWED_SORT_FIELDS.contains(prop)) {
                throw new InvalidRequestException("Such property is not supported for sorting " + prop);
            }
        }
    }

    private Pageable addFallbackSort(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id").ascending());
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
