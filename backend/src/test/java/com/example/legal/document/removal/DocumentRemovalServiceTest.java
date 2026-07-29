package com.example.legal.document.removal;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRemovalServiceTest {

    @Mock
    private CaseDocumentRepository documentRepository;
    @Mock
    private DocumentStorage documentStorage;

    private DocumentRemovalService service;

    @BeforeEach
    void setUp() {
        service = new DocumentRemovalService(documentRepository, documentStorage);
    }

    @Test
    void flushesMetadataDeletionBeforeRemovingBinaryContent() {
        CaseDocumentEntity document = document(DocumentSource.UPLOADED);
        when(documentRepository.findByIdAndCaseId(4L, 7L))
                .thenReturn(Optional.of(document));

        service.remove(7L, 4L);

        InOrder order = inOrder(documentRepository, documentStorage);
        order.verify(documentRepository).delete(document);
        order.verify(documentRepository).flush();
        order.verify(documentStorage).remove("storage-key");
    }

    @Test
    void removesGeneratedDocumentsWithoutUsingOriginAsAuthorization() {
        CaseDocumentEntity document = document(DocumentSource.GENERATED);
        when(documentRepository.findByIdAndCaseId(4L, 7L))
                .thenReturn(Optional.of(document));

        service.remove(7L, 4L);

        verify(documentStorage).remove("storage-key");
    }

    @Test
    void rejectsMissingOrCrossCaseDocumentWithoutChangingStorage() {
        when(documentRepository.findByIdAndCaseId(4L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(7L, 4L))
                .isInstanceOf(DocumentRemovalNotFoundException.class);

        verify(documentRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(documentRepository, never()).flush();
        verify(documentStorage, never()).remove(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void propagatesStorageFailureSoTheDatabaseTransactionCanRollBack() {
        CaseDocumentEntity document = document(DocumentSource.UPLOADED);
        DocumentStorageException failure =
                new DocumentStorageException("test removal failure");
        when(documentRepository.findByIdAndCaseId(4L, 7L))
                .thenReturn(Optional.of(document));
        org.mockito.Mockito.doThrow(failure)
                .when(documentStorage).remove("storage-key");

        assertThatThrownBy(() -> service.remove(7L, 4L))
                .isSameAs(failure);
    }

    private CaseDocumentEntity document(DocumentSource source) {
        return new CaseDocumentEntity(
                7L,
                "evidence.pdf",
                "storage-key",
                source,
                DocumentFormat.PDF,
                "application/pdf",
                12
        );
    }
}
