package com.example.legal.document.upload;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;
import com.example.legal.legalcase.CaseRepository;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import com.example.legal.document.storage.DocumentStorageRequest;
import com.example.legal.document.storage.StoredDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseDocumentRepository documentRepository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private DocumentUploadValidator validator;

    private DocumentUploadService service;

    @BeforeEach
    void setUp() {
        service = new DocumentUploadService(
                caseRepository,
                documentRepository,
                documentStorage,
                validator
        );
    }

    @Test
    void storesContentAndPersistsMetadata() {
        DocumentUploadCommand command = command();
        ValidatedDocumentUpload validated = validated();
        CaseDocumentEntity saved = new CaseDocumentEntity(
                7L,
                "pleading.pdf",
                "storage-key",
                DocumentSource.UPLOADED,
                DocumentFormat.PDF,
                "application/pdf",
                12
        );
        when(caseRepository.existsById(7L)).thenReturn(true);
        when(validator.validate(command)).thenReturn(validated);
        when(documentStorage.store(any())).thenReturn(new StoredDocument("storage-key", 12));
        when(documentRepository.saveAndFlush(any())).thenReturn(saved);

        CaseDocumentResponse response = service.upload(7L, command);

        assertThat(response.caseId()).isEqualTo(7L);
        assertThat(response.originalFileName()).isEqualTo("pleading.pdf");
        assertThat(response.documentSource()).isEqualTo(DocumentSource.UPLOADED);
        assertThat(response.fileFormat()).isEqualTo(DocumentFormat.PDF);
        assertThat(response.fileSize()).isEqualTo(12);
        verify(documentStorage).store(any(DocumentStorageRequest.class));
        verify(documentRepository).saveAndFlush(any(CaseDocumentEntity.class));
    }

    @Test
    void rejectsMissingCaseBeforeInspectingOrStoringContent() {
        DocumentUploadCommand command = command();
        when(caseRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.upload(99L, command))
                .isInstanceOf(DocumentCaseNotFoundException.class);

        verify(validator, never()).validate(any());
        verify(documentStorage, never()).store(any());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    @Test
    void removesStoredContentWhenMetadataPersistenceFails() {
        DocumentUploadCommand command = command();
        RuntimeException persistenceFailure = new RuntimeException("test persistence failure");
        prepareStorage(command);
        when(documentRepository.saveAndFlush(any())).thenThrow(persistenceFailure);

        assertThatThrownBy(() -> service.upload(7L, command))
                .isSameAs(persistenceFailure);

        verify(documentStorage).remove("storage-key");
    }

    @Test
    void preservesPersistenceFailureWhenCompensatingRemovalAlsoFails() {
        DocumentUploadCommand command = command();
        RuntimeException persistenceFailure = new RuntimeException("test persistence failure");
        prepareStorage(command);
        when(documentRepository.saveAndFlush(any())).thenThrow(persistenceFailure);
        doThrow(new DocumentStorageException("test cleanup failure"))
                .when(documentStorage).remove("storage-key");

        assertThatThrownBy(() -> service.upload(7L, command))
                .isSameAs(persistenceFailure);
    }

    @Test
    void normalizesFailureToOpenContentForStorage() {
        DocumentUploadCommand command = new DocumentUploadCommand(
                "pleading.pdf",
                "application/pdf",
                12,
                () -> {
                    throw new IOException("test failure");
                }
        );
        when(caseRepository.existsById(7L)).thenReturn(true);
        when(validator.validate(command)).thenReturn(validated());

        assertThatThrownBy(() -> service.upload(7L, command))
                .isInstanceOf(DocumentUploadProcessingException.class)
                .hasCauseInstanceOf(IOException.class);

        verify(documentStorage, never()).store(any());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    @Test
    void closesContentStreamAfterStorageReturns() {
        TrackingInputStream content = new TrackingInputStream(new byte[12]);
        DocumentUploadCommand command = new DocumentUploadCommand(
                "pleading.pdf",
                "application/pdf",
                12,
                () -> content
        );
        prepareStorage(command);
        when(documentRepository.saveAndFlush(any())).thenReturn(new CaseDocumentEntity(
                7L,
                "pleading.pdf",
                "storage-key",
                DocumentSource.UPLOADED,
                DocumentFormat.PDF,
                "application/pdf",
                12
        ));

        service.upload(7L, command);

        assertThat(content.closed).isTrue();
    }

    private void prepareStorage(DocumentUploadCommand command) {
        when(caseRepository.existsById(7L)).thenReturn(true);
        when(validator.validate(command)).thenReturn(validated());
        when(documentStorage.store(any())).thenReturn(new StoredDocument("storage-key", 12));
    }

    private DocumentUploadCommand command() {
        return new DocumentUploadCommand(
                "pleading.pdf",
                "application/pdf",
                12,
                () -> new ByteArrayInputStream(new byte[12])
        );
    }

    private ValidatedDocumentUpload validated() {
        return new ValidatedDocumentUpload(
                "pleading.pdf",
                DocumentFormat.PDF,
                "application/pdf",
                12
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
