package com.example.mvc2.controllers;

import com.example.mvc2.dtos.authors.AuthorRequest;
import com.example.mvc2.dtos.authors.AuthorResponse;
import com.example.mvc2.services.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
public class AuthorController {
    private final AuthorService authorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse create(@Valid @RequestBody AuthorRequest request) {
        return authorService.create(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorResponse getOne(@PathVariable("id") Long authorId) {
        return authorService.getOne(authorId);
    }
}
