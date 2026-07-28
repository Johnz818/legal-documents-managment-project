package com.example.legal.document.upload;

public class DocumentTooLargeException extends RuntimeException {

    public DocumentTooLargeException() {
        super("Document exceeds the configured size limit");
    }
}
