package com.example.mvc2.services;

import com.example.mvc2.dtos.books.BookRequest;
import com.example.mvc2.dtos.books.BookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookService {
    public BookResponse create(BookRequest request) {

    }

    public BookResponse getOne(Long bookId) {

    }

    public Page<BookResponse> getList(Pageable pageable) {

    }

    public BookResponse update(Long bookId, BookRequest request) {

    }

    public void deleteSoft(Long bookId) {

    }
}
