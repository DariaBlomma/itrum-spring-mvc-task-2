package com.example.mvc2.controllers;

import com.example.mvc2.dtos.books.BookRequest;
import com.example.mvc2.dtos.books.BookResponse;
import com.example.mvc2.services.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return bookService.create(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse getOne( @PathVariable("id") Long bookId) {

    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<BookResponse> getList(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
           Pageable pageable
    ) {

    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse update(@PathVariable("id") Long bookId, @Valid @RequestBody BookRequest request) {
        return bookService.update(bookId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete() {

    }
}
