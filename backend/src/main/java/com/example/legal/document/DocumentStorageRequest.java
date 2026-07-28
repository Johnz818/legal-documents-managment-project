package com.example.legal.document;

import java.io.InputStream;
import java.util.Objects;

/**
 * Binary content submitted to a {@link DocumentStorage} implementation. The
 * content length is the number of bytes the implementation is expected to
 * consume from the caller-owned stream.
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
