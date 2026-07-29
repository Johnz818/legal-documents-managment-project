package com.example.legal.document.removal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DocumentRemovalExceptionHandler {

    @ExceptionHandler(DocumentRemovalNotFoundException.class)
    ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }
}
