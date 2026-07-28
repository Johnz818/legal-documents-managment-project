package com.example.legal.document.storage;

/**
 * Provider-neutral facts returned after document content is stored.
 */
public record StoredDocument(
        String storageKey,
        long contentLength
) {

    public StoredDocument {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey must not be blank");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative");
        }
    }
}
