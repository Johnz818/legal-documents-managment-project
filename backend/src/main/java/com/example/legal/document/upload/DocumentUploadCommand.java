package com.example.legal.document.upload;

public record DocumentUploadCommand(
        String originalFileName,
        String contentType,
        long contentLength,
        DocumentContentSource contentSource
) {
}
