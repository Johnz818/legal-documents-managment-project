package com.example.legal.document;

import java.io.InputStream;

/**
 * Stores and retrieves document binary content independently of its business
 * metadata and the underlying storage provider.
 */
public interface DocumentStorage {

    /**
     * Stores the supplied content under a backend-generated opaque key.
     *
     * @param request content to store; the caller remains responsible for
     *                closing its input stream
     * @return facts about the stored content
     * @throws DocumentStorageException when the content cannot be stored
     */
    StoredDocument store(DocumentStorageRequest request);

    /**
     * Opens stored content for streaming.
     *
     * @param storageKey opaque key returned by {@link #store(DocumentStorageRequest)}
     * @return stream that must be closed by the caller
     * @throws DocumentStorageException when the content cannot be opened
     */
    InputStream open(String storageKey);

    /**
     * Removes stored content. Implementations must treat an already absent key
     * as successfully removed so compensating cleanup can be retried.
     *
     * @param storageKey opaque key returned by {@link #store(DocumentStorageRequest)}
     * @throws DocumentStorageException when removal fails for another reason
     */
    void remove(String storageKey);
}
