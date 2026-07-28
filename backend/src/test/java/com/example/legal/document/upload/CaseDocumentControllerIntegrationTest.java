package com.example.legal.document.upload;

import com.example.legal.legalcase.CaseEntity;
import com.example.legal.legalcase.CaseRepository;
import com.example.legal.legalcase.CaseStatus;
import com.example.legal.document.storage.DocumentStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseDocumentControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final CaseRepository caseRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DocumentStorage documentStorage;

    @Autowired
    CaseDocumentControllerIntegrationTest(
            MockMvc mockMvc,
            CaseRepository caseRepository,
            JdbcTemplate jdbcTemplate,
            DocumentStorage documentStorage
    ) {
        this.mockMvc = mockMvc;
        this.caseRepository = caseRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.documentStorage = documentStorage;
    }

    @Test
    void uploadsDocumentContentAndPersistsMetadata() throws Exception {
        Long caseId = createCase();
        byte[] content = "%PDF-1.7 test evidence".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "证据材料.pdf",
                "application/pdf",
                content
        );
        String storageKey = null;

        try {
            mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseId).file(file))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.caseId").value(caseId))
                    .andExpect(jsonPath("$.originalFileName").value("证据材料.pdf"))
                    .andExpect(jsonPath("$.documentSource").value("UPLOADED"))
                    .andExpect(jsonPath("$.fileFormat").value("PDF"))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.fileSize").value(content.length))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.storageKey").doesNotExist());

            storageKey = jdbcTemplate.queryForObject(
                    "SELECT storage_key FROM case_documents WHERE case_id = ?",
                    String.class,
                    caseId
            );
            try (InputStream storedContent = documentStorage.open(storageKey)) {
                assertThat(storedContent.readAllBytes()).isEqualTo(content);
            }
        } finally {
            if (storageKey != null) {
                documentStorage.remove(storageKey);
            }
        }
    }

    @Test
    void returnsNotFoundWhenCaseDoesNotExist() throws Exception {
        MockMultipartFile file = pdfFile("%PDF-test".getBytes());

        mockMvc.perform(multipart("/api/cases/{caseId}/documents", Long.MAX_VALUE).file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsEmptyDocument() throws Exception {
        Long caseId = createCase();

        mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseId)
                        .file(pdfFile(new byte[0])))
                .andExpect(status().isBadRequest());

        assertThat(documentCount(caseId)).isZero();
    }

    @Test
    void rejectsUnsupportedOrMismatchedDocument() throws Exception {
        Long caseId = createCase();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.pdf",
                "application/pdf",
                "not really a PDF".getBytes()
        );

        mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseId).file(file))
                .andExpect(status().isUnsupportedMediaType());

        assertThat(documentCount(caseId)).isZero();
    }

    @Test
    void rejectsDocumentLargerThanFiveMegabytes() throws Exception {
        Long caseId = createCase();
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy("%PDF-".getBytes(), 0, content, 0, 5);

        mockMvc.perform(multipart("/api/cases/{caseId}/documents", caseId)
                        .file(pdfFile(content)))
                .andExpect(status().isPayloadTooLarge());

        assertThat(documentCount(caseId)).isZero();
    }

    private MockMultipartFile pdfFile(byte[] content) {
        return new MockMultipartFile("file", "evidence.pdf", "application/pdf", content);
    }

    private Long createCase() {
        return caseRepository.saveAndFlush(new CaseEntity(
                "DOC-UPLOAD-" + UUID.randomUUID(),
                "Document upload test case",
                CaseStatus.PENDING_FILING,
                "Test plaintiff",
                "Test defendant",
                "Test lawyer"
        )).getId();
    }

    private long documentCount(Long caseId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM case_documents WHERE case_id = ?",
                Long.class,
                caseId
        );
        return count == null ? 0 : count;
    }
}
