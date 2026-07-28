package com.example.legal.document.read;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;
import com.example.legal.document.storage.DocumentStorage;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseDocumentReadControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final CaseRepository caseRepository;
    private final CaseDocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final JdbcTemplate jdbcTemplate;
    private final List<String> storedKeys = new ArrayList<>();

    @Autowired
    CaseDocumentReadControllerIntegrationTest(
            MockMvc mockMvc,
            CaseRepository caseRepository,
            CaseDocumentRepository documentRepository,
            DocumentStorage documentStorage,
            JdbcTemplate jdbcTemplate
    ) {
        this.mockMvc = mockMvc;
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void removeStoredTestContent() {
        storedKeys.forEach(documentStorage::remove);
    }

    @Test
    void listsCaseDocumentsNewestFirstWithoutStorageKeys() throws Exception {
        Long caseId = createCase();
        CaseDocumentEntity older = createDocument(caseId, "older.pdf", "%PDF-older".getBytes());
        CaseDocumentEntity newer = createDocument(caseId, "newer.pdf", "%PDF-newer".getBytes());
        jdbcTemplate.update(
                "UPDATE case_documents SET created_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                older.getId()
        );
        jdbcTemplate.update(
                "UPDATE case_documents SET created_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 1, 2, 10, 0),
                newer.getId()
        );

        mockMvc.perform(get("/api/cases/{caseId}/documents", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(newer.getId()))
                .andExpect(jsonPath("$.data[0].caseId").value(caseId))
                .andExpect(jsonPath("$.data[0].originalFileName").value("newer.pdf"))
                .andExpect(jsonPath("$.data[0].documentSource").value("UPLOADED"))
                .andExpect(jsonPath("$.data[0].fileFormat").value("PDF"))
                .andExpect(jsonPath("$.data[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data[0].fileSize").value(10))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].updatedAt").exists())
                .andExpect(jsonPath("$.data[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.data[1].id").value(older.getId()));
    }

    @Test
    void returnsEmptyListForExistingCaseWithoutDocuments() throws Exception {
        Long caseId = createCase();

        mockMvc.perform(get("/api/cases/{caseId}/documents", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsNotFoundWhenListingDocumentsForMissingCase() throws Exception {
        mockMvc.perform(get("/api/cases/{caseId}/documents", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadsExactContentWithSafeMetadataHeaders() throws Exception {
        Long caseId = createCase();
        byte[] content = "%PDF-中文证据".getBytes();
        CaseDocumentEntity document = createDocument(caseId, "证据 材料.pdf", content);

        String disposition = mockMvc.perform(get(
                        "/api/cases/{caseId}/documents/{documentId}/content",
                        caseId,
                        document.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().longValue("Content-Length", content.length))
                .andExpect(content().bytes(content))
                .andReturn()
                .getResponse()
                .getHeader("Content-Disposition");

        assertThat(disposition)
                .startsWith("attachment;")
                .contains("filename*=UTF-8''");
    }

    @Test
    void returnsNotFoundForMissingDocument() throws Exception {
        Long caseId = createCase();

        mockMvc.perform(get(
                        "/api/cases/{caseId}/documents/{documentId}/content",
                        caseId,
                        Long.MAX_VALUE
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void doesNotDownloadDocumentOwnedByAnotherCase() throws Exception {
        Long requestedCaseId = createCase();
        Long owningCaseId = createCase();
        CaseDocumentEntity document = createDocument(
                owningCaseId,
                "private.pdf",
                "%PDF-private".getBytes()
        );

        mockMvc.perform(get(
                        "/api/cases/{caseId}/documents/{documentId}/content",
                        requestedCaseId,
                        document.getId()
                ))
                .andExpect(status().isNotFound());
    }

    private Long createCase() {
        return caseRepository.saveAndFlush(new CaseEntity(
                "DOC-READ-" + UUID.randomUUID(),
                "Document read test case",
                CaseStatus.PENDING_FILING,
                "Test plaintiff",
                "Test defendant",
                "Test lawyer"
        )).getId();
    }

    private CaseDocumentEntity createDocument(
            Long caseId,
            String fileName,
            byte[] content
    ) {
        StoredDocument stored = documentStorage.store(new DocumentStorageRequest(
                new ByteArrayInputStream(content),
                content.length
        ));
        storedKeys.add(stored.storageKey());
        return documentRepository.saveAndFlush(new CaseDocumentEntity(
                caseId,
                fileName,
                stored.storageKey(),
                DocumentSource.UPLOADED,
                DocumentFormat.PDF,
                "application/pdf",
                content.length
        ));
    }
}
