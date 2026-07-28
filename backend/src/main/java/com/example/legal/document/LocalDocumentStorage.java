package com.example.legal.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores document content beneath a private local directory using opaque UUID
 * keys. Intended for local development, not direct public file serving.
 */
public final class LocalDocumentStorage implements DocumentStorage {

    private static final String STORE_FAILURE = "Unable to store document content";
    private static final String OPEN_FAILURE = "Unable to open document content";
    private static final String REMOVE_FAILURE = "Unable to remove document content";
    private static final String INVALID_KEY = "Invalid document storage key";

    private final Path storageRoot;

    public LocalDocumentStorage(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        try {
            Files.createDirectories(storageRoot);
            this.storageRoot = storageRoot.toRealPath();
        } catch (IOException exception) {
            throw new DocumentStorageException(
                    "Unable to initialize document storage",
                    exception
            );
        }
    }

    @Override
    public StoredDocument store(DocumentStorageRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Path temporaryFile = null;

        try {
            temporaryFile = Files.createTempFile(storageRoot, ".document-", ".tmp");
            long storedLength;
            try (OutputStream output = Files.newOutputStream(
                    temporaryFile,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                storedLength = request.content().transferTo(output);
            }

            if (storedLength != request.contentLength()) {
                throw new DocumentStorageException(
                        "Stored content length does not match expected length"
                );
            }

            String storageKey = UUID.randomUUID().toString();
            Path storedFile = resolveStorageKey(storageKey);
            moveIntoPlace(temporaryFile, storedFile);
            temporaryFile = null;

            return new StoredDocument(storageKey, storedLength);
        } catch (IOException exception) {
            throw new DocumentStorageException(STORE_FAILURE, exception);
        } finally {
            removeTemporaryFile(temporaryFile);
        }
    }

    @Override
    public InputStream open(String storageKey) {
        Path storedFile = resolveStorageKey(storageKey);
        if (Files.isSymbolicLink(storedFile)) {
            throw new DocumentStorageException(OPEN_FAILURE);
        }

        try {
            return Files.newInputStream(storedFile, StandardOpenOption.READ);
        } catch (IOException exception) {
            throw new DocumentStorageException(OPEN_FAILURE, exception);
        }
    }

    @Override
    public void remove(String storageKey) {
        Path storedFile = resolveStorageKey(storageKey);
        try {
            Files.deleteIfExists(storedFile);
        } catch (IOException exception) {
            throw new DocumentStorageException(REMOVE_FAILURE, exception);
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null) {
            throw new DocumentStorageException(INVALID_KEY);
        }

        try {
            UUID parsedKey = UUID.fromString(storageKey);
            if (!parsedKey.toString().equals(storageKey)) {
                throw new DocumentStorageException(INVALID_KEY);
            }
        } catch (IllegalArgumentException exception) {
            throw new DocumentStorageException(INVALID_KEY);
        }

        Path resolved = storageRoot.resolve(storageKey).normalize();
        return resolved;
    }

    private void moveIntoPlace(Path temporaryFile, Path storedFile) throws IOException {
        try {
            Files.move(temporaryFile, storedFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, storedFile);
        }
    }

    private void removeTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // The original storage failure remains the actionable error.
        }
    }
}
