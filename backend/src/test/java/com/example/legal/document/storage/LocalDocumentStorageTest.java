package com.example.legal.document.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDocumentStorageTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void storesAndOpensIdenticalContentUnderOpaqueKey() throws IOException {
        LocalDocumentStorage storage = storage();
        byte[] content = "legal document content".getBytes(StandardCharsets.UTF_8);

        StoredDocument stored = storage.store(request(content));

        assertThat(UUID.fromString(stored.storageKey()).toString())
                .isEqualTo(stored.storageKey());
        assertThat(stored.contentLength()).isEqualTo(content.length);
        assertThat(temporaryDirectory.resolve(stored.storageKey()))
                .exists()
                .isRegularFile();
        try (InputStream opened = storage.open(stored.storageKey())) {
            assertThat(opened.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void initializesMissingStorageRoot() {
        Path missingRoot = temporaryDirectory.resolve("nested/documents");

        new LocalDocumentStorage(missingRoot);

        assertThat(missingRoot).isDirectory();
    }

    @Test
    void storesEmptyContentAtStorageBoundary() throws IOException {
        LocalDocumentStorage storage = storage();

        StoredDocument stored = storage.store(request(new byte[0]));

        assertThat(stored.contentLength()).isZero();
        try (InputStream opened = storage.open(stored.storageKey())) {
            assertThat(opened.readAllBytes()).isEmpty();
        }
    }

    @Test
    void doesNotCloseCallerProvidedStream() {
        TrackingInputStream content = new TrackingInputStream(new byte[]{1, 2, 3});
        LocalDocumentStorage storage = storage();

        storage.store(new DocumentStorageRequest(content, 3));

        assertThat(content.closed).isFalse();
    }

    @Test
    void removesContentIdempotently() {
        LocalDocumentStorage storage = storage();
        StoredDocument stored = storage.store(request(new byte[]{1}));

        storage.remove(stored.storageKey());
        storage.remove(stored.storageKey());

        assertThat(temporaryDirectory.resolve(stored.storageKey())).doesNotExist();
        assertThatThrownBy(() -> storage.open(stored.storageKey()))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Unable to open document content");
    }

    @Test
    void rejectsLengthMismatchAndRemovesTemporaryContent() throws IOException {
        LocalDocumentStorage storage = storage();

        assertThatThrownBy(() -> storage.store(new DocumentStorageRequest(
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                4
        )))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Stored content length does not match expected length");
        try (var files = Files.list(temporaryDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void translatesInterruptedInputAndRemovesTemporaryContent() throws IOException {
        LocalDocumentStorage storage = storage();
        InputStream interruptedContent = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("interrupted");
            }
        };

        assertThatThrownBy(() -> storage.store(
                new DocumentStorageRequest(interruptedContent, 1)
        ))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Unable to store document content")
                .hasCauseInstanceOf(IOException.class);
        try (var files = Files.list(temporaryDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void rejectsInvalidStorageKeys() {
        LocalDocumentStorage storage = storage();

        assertThatThrownBy(() -> storage.open(null))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Invalid document storage key");
        assertThatThrownBy(() -> storage.open(""))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Invalid document storage key");
        assertThatThrownBy(() -> storage.open("../outside"))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Invalid document storage key");
        assertThatThrownBy(() -> storage.open("1-1-1-1-1"))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Invalid document storage key");
    }

    @Test
    void rejectsSymbolicLinkContent() throws IOException {
        LocalDocumentStorage storage = storage();
        String storageKey = UUID.randomUUID().toString();
        Path outsideFile = temporaryDirectory.resolveSibling("outside-" + storageKey);
        Files.write(outsideFile, new byte[]{1});
        Files.createSymbolicLink(temporaryDirectory.resolve(storageKey), outsideFile);

        try {
            assertThatThrownBy(() -> storage.open(storageKey))
                    .isInstanceOf(DocumentStorageException.class)
                    .hasMessage("Unable to open document content");
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    @Test
    void translatesRemovalFailureWithoutExposingStoragePath() throws IOException {
        LocalDocumentStorage storage = storage();
        String storageKey = UUID.randomUUID().toString();
        Path nonEmptyDirectory = temporaryDirectory.resolve(storageKey);
        Files.createDirectory(nonEmptyDirectory);
        Files.write(nonEmptyDirectory.resolve("content"), new byte[]{1});

        assertThatThrownBy(() -> storage.remove(storageKey))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Unable to remove document content")
                .hasCauseInstanceOf(IOException.class)
                .message()
                .doesNotContain(temporaryDirectory.toString());
    }

    @Test
    void rejectsStorageRootThatIsAFileWithoutExposingItsPath() throws IOException {
        Path invalidRoot = temporaryDirectory.resolve("not-a-directory");
        Files.write(invalidRoot, new byte[]{1});

        assertThatThrownBy(() -> new LocalDocumentStorage(invalidRoot))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("Unable to initialize document storage")
                .message()
                .doesNotContain(invalidRoot.toString());
    }

    @Test
    void rejectsNullStorageRootAndRequest() {
        LocalDocumentStorage storage = storage();

        assertThatThrownBy(() -> new LocalDocumentStorage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("storageRoot must not be null");
        assertThatThrownBy(() -> storage.store(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("request must not be null");
    }

    private LocalDocumentStorage storage() {
        return new LocalDocumentStorage(temporaryDirectory);
    }

    private DocumentStorageRequest request(byte[] content) {
        return new DocumentStorageRequest(
                new ByteArrayInputStream(content),
                content.length
        );
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] content) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
