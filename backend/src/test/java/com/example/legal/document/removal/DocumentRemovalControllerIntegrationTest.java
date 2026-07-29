package com.example.legal.document.removal;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import com.example.legal.document.storage.DocumentStorageRequest;
import com.example.legal.document.storage.StoredDocument;
import com.example.legal.legalcase.CaseEntity;
import com.example.legal.legalcase.CaseRepository;
import com.example.legal.legalcase.CaseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentRemovalControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final CaseRepository caseRepository;
    private final CaseDocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final List<String> storedKeys = new ArrayList<>();

    @Autowired
    DocumentRemovalControllerIntegrationTest(
            MockMvc mockMvc,
            CaseRepository caseRepository,
            CaseDocumentRepository documentRepository,
            DocumentStorage documentStorage
    ) {
        this.mockMvc = mockMvc;
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
    }

    @AfterEach
    void removeStoredTestContent() {
        storedKeys.forEach(documentStorage::remove);
    }

    @Test
    void permanentlyRemovesMetadataAndBinaryContent() throws Exception {
        Long caseId = createCase();
        CaseDocumentEntity document = createDocument(caseId, DocumentSource.UPLOADED);

        mockMvc.perform(delete(
                        "/api/cases/{caseId}/documents/{documentId}",
                        caseId,
                        document.getId()
                ))
                .andExpect(status().isNoContent());

        assertThat(documentRepository.findById(document.getId())).isEmpty();
        assertThatThrownBy(() -> documentStorage.open(document.getStorageKey()))
                .isInstanceOf(DocumentStorageException.class);
    }

    @Test
    void removesGeneratedDocumentWithoutOriginBasedRestriction() throws Exception {
        Long caseId = createCase();
        CaseDocumentEntity document = createDocument(caseId, DocumentSource.GENERATED);

        mockMvc.perform(delete(
                        "/api/cases/{caseId}/documents/{documentId}",
                        caseId,
                        document.getId()
                ))
                .andExpect(status().isNoContent());

        assertThat(documentRepository.findById(document.getId())).isEmpty();
    }

    @Test
    void returnsNotFoundForMissingDocument() throws Exception {
        Long caseId = createCase();

        mockMvc.perform(delete(
                        "/api/cases/{caseId}/documents/{documentId}",
                        caseId,
                        Long.MAX_VALUE
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void doesNotRemoveDocumentOwnedByAnotherCase() throws Exception {
        Long requestedCaseId = createCase();
        Long owningCaseId = createCase();
        CaseDocumentEntity document =
                createDocument(owningCaseId, DocumentSource.UPLOADED);

        mockMvc.perform(delete(
                        "/api/cases/{caseId}/documents/{documentId}",
                        requestedCaseId,
                        document.getId()
                ))
                .andExpect(status().isNotFound());

        assertThat(documentRepository.findById(document.getId())).isPresent();
        try (var content = documentStorage.open(document.getStorageKey())) {
            assertThat(content.readAllBytes()).isNotEmpty();
        }
    }

    private Long createCase() {
        return caseRepository.saveAndFlush(new CaseEntity(
                "DOC-REMOVE-" + UUID.randomUUID(),
                "Document removal test case",
                CaseStatus.PENDING_FILING,
                "Test plaintiff",
                "Test defendant",
                "Test lawyer"
        )).getId();
    }

    private CaseDocumentEntity createDocument(
            Long caseId,
            DocumentSource source
    ) {
        byte[] content = "%PDF-removal-test".getBytes();
        StoredDocument stored = documentStorage.store(new DocumentStorageRequest(
                new ByteArrayInputStream(content),
                content.length
        ));
        storedKeys.add(stored.storageKey());
        return documentRepository.saveAndFlush(new CaseDocumentEntity(
                caseId,
                "evidence.pdf",
                stored.storageKey(),
                source,
                DocumentFormat.PDF,
                "application/pdf",
                content.length
        ));
    }
}
