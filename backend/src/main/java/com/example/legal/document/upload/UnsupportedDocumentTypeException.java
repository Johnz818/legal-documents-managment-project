package com.example.legal.document.upload;

public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException() {
        super("Unsupported document type");
    }
}
