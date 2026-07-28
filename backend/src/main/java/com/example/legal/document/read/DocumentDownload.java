package com.example.legal.document.read;

import java.io.InputStream;

record DocumentDownload(
        String originalFileName,
        String contentType,
        long contentLength,
        InputStream content
) {
}
