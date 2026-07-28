package com.example.legal.document.upload;

import com.example.legal.document.DocumentFormat;

record ValidatedDocumentUpload(
        String originalFileName,
        DocumentFormat fileFormat,
        String contentType,
        long contentLength
) {
}
