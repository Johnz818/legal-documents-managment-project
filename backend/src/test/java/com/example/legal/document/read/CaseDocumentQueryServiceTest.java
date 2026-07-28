package com.example.legal.document.read;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import com.example.legal.legalcase.CaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDocumentQueryServiceTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseDocumentRepository documentRepository;
    @Mock
    private DocumentStorage documentStorage;

    private CaseDocumentQueryService service;

    @BeforeEach
    void setUp() {
        service = new CaseDocumentQueryService(
                caseRepository,
                documentRepository,
                documentStorage
        );
    }

    @Test
    void returnsMappedDocumentsFromRepositoryOrder() {
        CaseDocumentEntity first = document("first.pdf", "documents/first");
        CaseDocumentEntity second = document("second.docx", "documents/second");
        when(caseRepository.existsById(7L)).thenReturn(true);
        when(documentRepository.findAllByCaseIdOrderByCreatedAtDescIdDesc(7L))
                .thenReturn(List.of(first, second));

        CaseDocumentListResponse response = service.getCaseDocuments(7L);

        assertThat(response.data())
                .extracting(CaseDocumentSummaryResponse::originalFileName)
                .containsExactly("first.pdf", "second.docx");
        assertThat(response.data().getFirst().caseId()).isEqualTo(7L);
        assertThat(response.data().getFirst().documentSource()).isEqualTo(DocumentSource.UPLOADED);
        assertThat(response.data().getFirst().fileFormat()).isEqualTo(DocumentFormat.PDF);
    }

    @Test
    void returnsEmptyListForExistingCaseWithoutDocuments() {
        when(caseRepository.existsById(7L)).thenReturn(true);
        when(documentRepository.findAllByCaseIdOrderByCreatedAtDescIdDesc(7L))
                .thenReturn(List.of());

        assertThat(service.getCaseDocuments(7L).data()).isEmpty();
    }

    @Test
    void rejectsMissingCaseBeforeQueryingDocuments() {
        when(caseRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getCaseDocuments(99L))
                .isInstanceOf(DocumentReadNotFoundException.class);

        verify(documentRepository, never())
                .findAllByCaseIdOrderByCreatedAtDescIdDesc(99L);
    }

    @Test
    void opensContentForDocumentOwnedByCase() throws Exception {
        CaseDocumentEntity document = document("evidence.pdf", "documents/evidence");
        ByteArrayInputStream content = new ByteArrayInputStream("%PDF-test".getBytes());
        when(documentRepository.findByIdAndCaseId(4L, 7L)).thenReturn(Optional.of(document));
        when(documentStorage.open("documents/evidence")).thenReturn(content);

        DocumentDownload download = service.download(7L, 4L);

        assertThat(download.originalFileName()).isEqualTo("evidence.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
        assertThat(download.contentLength()).isEqualTo(9);
        assertThat(download.content().readAllBytes()).isEqualTo("%PDF-test".getBytes());
    }

    @Test
    void rejectsMissingOrCrossCaseDocumentBeforeOpeningStorage() {
        when(documentRepository.findByIdAndCaseId(4L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(7L, 4L))
                .isInstanceOf(DocumentReadNotFoundException.class);

        verify(documentStorage, never()).open("documents/evidence");
    }

    @Test
    void propagatesStorageFailure() {
        CaseDocumentEntity document = document("evidence.pdf", "documents/evidence");
        DocumentStorageException failure = new DocumentStorageException("test failure");
        when(documentRepository.findByIdAndCaseId(4L, 7L)).thenReturn(Optional.of(document));
        when(documentStorage.open("documents/evidence")).thenThrow(failure);

        assertThatThrownBy(() -> service.download(7L, 4L))
                .isSameAs(failure);
    }

    private CaseDocumentEntity document(String fileName, String storageKey) {
        return new CaseDocumentEntity(
                7L,
                fileName,
                storageKey,
                DocumentSource.UPLOADED,
                fileName.endsWith(".docx") ? DocumentFormat.DOCX : DocumentFormat.PDF,
                fileName.endsWith(".docx")
                        ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        : "application/pdf",
                9
        );
    }
}
