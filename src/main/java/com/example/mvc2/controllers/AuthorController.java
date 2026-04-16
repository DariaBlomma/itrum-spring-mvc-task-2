package com.example.mvc2.controllers;

import com.example.mvc2.dtos.authors.AuthorRequest;
import com.example.mvc2.dtos.authors.AuthorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authors")
public class AuthorController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse create(@Valid @RequestBody AuthorRequest request) {

    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorResponse getOne(@PathVariable("id") Long authorId) {

    }
}
