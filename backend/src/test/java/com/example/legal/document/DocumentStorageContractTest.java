package com.example.legal.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DocumentStorageContractTest {

    @Test
    void preservesValidStorageRequestValues() {
        ByteArrayInputStream content = new ByteArrayInputStream(new byte[]{1, 2, 3});

        DocumentStorageRequest request = new DocumentStorageRequest(content, 3);

        assertThat(request.content()).isSameAs(content);
        assertThat(request.contentLength()).isEqualTo(3);
    }

    @Test
    void permitsEmptyContentAtStorageBoundary() {
        DocumentStorageRequest request = new DocumentStorageRequest(
                new ByteArrayInputStream(new byte[0]),
                0
        );

        assertThat(request.contentLength()).isZero();
    }

    @Test
    void rejectsNullRequestContent() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DocumentStorageRequest(null, 0))
                .withMessage("content must not be null");
    }

    @Test
    void rejectsNegativeRequestContentLength() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DocumentStorageRequest(
                        new ByteArrayInputStream(new byte[0]),
                        -1
                ))
                .withMessage("contentLength must not be negative");
    }

    @Test
    void preservesValidStoredDocumentValues() {
        StoredDocument storedDocument = new StoredDocument("documents/opaque-key", 42);

        assertThat(storedDocument.storageKey()).isEqualTo("documents/opaque-key");
        assertThat(storedDocument.contentLength()).isEqualTo(42);
    }

    @Test
    void rejectsMissingStorageKey() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StoredDocument(null, 0))
                .withMessage("storageKey must not be blank");
    }

    @Test
    void rejectsBlankStorageKey() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StoredDocument(" \t", 0))
                .withMessage("storageKey must not be blank");
    }

    @Test
    void rejectsNegativeStoredContentLength() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StoredDocument("opaque-key", -1))
                .withMessage("contentLength must not be negative");
    }

    @Test
    void storageExceptionRetainsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("provider failed");

        DocumentStorageException withCause =
                new DocumentStorageException("Unable to store document", cause);
        DocumentStorageException withoutCause =
                new DocumentStorageException("Unable to open document");

        assertThat(withCause)
                .hasMessage("Unable to store document")
                .hasCause(cause);
        assertThat(withoutCause).hasMessage("Unable to open document");
    }
}
