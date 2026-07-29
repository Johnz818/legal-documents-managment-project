package com.example.legal.document.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class DocumentUploadExceptionHandler {

    @ExceptionHandler(InvalidDocumentUploadException.class)
    ResponseEntity<Void> handleInvalidUpload() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(DocumentCaseNotFoundException.class)
    ResponseEntity<Void> handleMissingCase() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({DocumentTooLargeException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<Void> handleOversizedUpload(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }

    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    ResponseEntity<Void> handleUnsupportedType() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }
}
