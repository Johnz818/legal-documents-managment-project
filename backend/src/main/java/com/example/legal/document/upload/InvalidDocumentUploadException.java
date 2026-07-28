package com.example.legal.document.upload;

public class InvalidDocumentUploadException extends RuntimeException {

    public InvalidDocumentUploadException(String message) {
        super(message);
    }
}
