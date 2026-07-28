package com.example.legal.document;

import java.io.InputStream;
import java.util.Objects;

/**
 * Binary content submitted to a {@link DocumentStorage} implementation.
 */
public record DocumentStorageRequest(
        InputStream content,
        long contentLength
) {

    public DocumentStorageRequest {
        Objects.requireNonNull(content, "content must not be null");
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative");
        }
    }
}
