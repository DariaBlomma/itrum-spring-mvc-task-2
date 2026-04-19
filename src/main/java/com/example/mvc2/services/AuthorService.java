package com.example.mvc2.services;

import com.example.mvc2.dtos.authors.AuthorRequest;
import com.example.mvc2.dtos.authors.AuthorResponse;
import com.example.mvc2.entities.Author;
import com.example.mvc2.entities.Book;
import com.example.mvc2.exceptions.InvalidRequestException;
import com.example.mvc2.exceptions.ResourceNotFoundException;
import com.example.mvc2.mappers.AuthorMapper;
import com.example.mvc2.repositories.AuthorRepository;
import com.example.mvc2.repositories.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;
    private final BookRepository bookRepository;

    @Transactional
    public AuthorResponse create(AuthorRequest request) {
        Author author = authorMapper.toEntity(request);
        Set<Long> requestIds = request.getBookIds();
        if (!requestIds.isEmpty()) {
            List<Book> activeBooks = bookRepository.findActiveByIds(request.getBookIds());
            checkBooksOfRequest(activeBooks, requestIds);
            author.setBooks(new HashSet<>(activeBooks));
        } else {
            author.setBooks(new HashSet<>());
        }
        Author saved = authorRepository.save(author);
        return authorMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthorResponse getOne(Long authorId) {
        Author author = authorRepository.findActiveById(authorId).orElseThrow(
                () -> new ResourceNotFoundException("Author does not exist or deleted with such id " + authorId)
        );
        return authorMapper.toResponse(author);
    }

    private void checkBooksOfRequest(List<Book> activeBooks, Set<Long> requestIds) {
        if (activeBooks.size() < requestIds.size()) {
            Set<Long> foundIds = activeBooks.stream()
                    .map(Book::getId)
                    .collect(Collectors.toSet());

            Set<Long> invalidIds = requestIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new InvalidRequestException("Some books do not exist or were deleted. Invalid ids are: " + invalidIds);
        }
    }
}
