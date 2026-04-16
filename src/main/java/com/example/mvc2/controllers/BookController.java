package com.example.mvc2.controllers;

import com.example.mvc2.dtos.books.BookRequest;
import com.example.mvc2.dtos.books.BookResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {

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

    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete() {

    }
}
