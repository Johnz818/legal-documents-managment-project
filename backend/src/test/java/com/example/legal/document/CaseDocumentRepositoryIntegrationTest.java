package com.example.legal.document;

import com.example.legal.legalcase.CaseEntity;
import com.example.legal.legalcase.CaseRepository;
import com.example.legal.legalcase.CaseStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CaseDocumentRepositoryIntegrationTest {

    private final CaseDocumentRepository documentRepository;
    private final CaseRepository caseRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    CaseDocumentRepositoryIntegrationTest(
            CaseDocumentRepository documentRepository,
            CaseRepository caseRepository,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate
    ) {
        this.documentRepository = documentRepository;
        this.caseRepository = caseRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void persistsAndRetrievesDocumentMetadata() {
        Long caseId = createCase();
        CaseDocumentEntity document = document(
                caseId,
                DocumentSource.UPLOADED,
                DocumentFormat.PDF
        );

        CaseDocumentEntity saved = documentRepository.saveAndFlush(document);
        Long documentId = saved.getId();
        entityManager.clear();

        CaseDocumentEntity retrieved = documentRepository.findById(documentId).orElseThrow();

        assertThat(retrieved.getCaseId()).isEqualTo(caseId);
        assertThat(retrieved.getOriginalFileName()).isEqualTo("证据材料.pdf");
        assertThat(retrieved.getStorageKey()).isEqualTo(document.getStorageKey());
        assertThat(retrieved.getDocumentSource()).isEqualTo(DocumentSource.UPLOADED);
        assertThat(retrieved.getFileFormat()).isEqualTo(DocumentFormat.PDF);
        assertThat(retrieved.getContentType()).isEqualTo("application/pdf");
        assertThat(retrieved.getFileSize()).isEqualTo(1024);
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();

        String source = jdbcTemplate.queryForObject(
                "SELECT document_source FROM case_documents WHERE id = ?",
                String.class,
                documentId
        );
        String format = jdbcTemplate.queryForObject(
                "SELECT file_format FROM case_documents WHERE id = ?",
                String.class,
                documentId
        );
        assertThat(source).isEqualTo("UPLOADED");
        assertThat(format).isEqualTo("PDF");
    }

    @Test
    void allowsMultipleDocumentsForOneCase() {
        Long caseId = createCase();
        documentRepository.save(document(caseId, DocumentSource.UPLOADED, DocumentFormat.PDF));
        documentRepository.save(document(caseId, DocumentSource.GENERATED, DocumentFormat.DOCX));
        documentRepository.flush();

        Long documentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM case_documents WHERE case_id = ?",
                Long.class,
                caseId
        );

        assertThat(documentCount).isEqualTo(2);
    }

    @Test
    void rejectsMissingCaseReference() {
        CaseDocumentEntity document = document(
                Long.MAX_VALUE,
                DocumentSource.UPLOADED,
                DocumentFormat.PDF
        );

        assertThatThrownBy(() -> documentRepository.saveAndFlush(document))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void preventsDeletingCaseWithDocumentMetadata() {
        Long caseId = createCase();
        documentRepository.saveAndFlush(
                document(caseId, DocumentSource.UPLOADED, DocumentFormat.PDF)
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM cases WHERE id = ?",
                caseId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateStorageKey() {
        Long caseId = createCase();
        String storageKey = uniqueStorageKey();
        documentRepository.saveAndFlush(new CaseDocumentEntity(
                caseId,
                "first.pdf",
                storageKey,
                DocumentSource.UPLOADED,
                DocumentFormat.PDF,
                "application/pdf",
                100
        ));

        CaseDocumentEntity duplicate = new CaseDocumentEntity(
                caseId,
                "second.pdf",
                storageKey,
                DocumentSource.UPLOADED,
                DocumentFormat.PDF,
                "application/pdf",
                200
        );

        assertThatThrownBy(() -> documentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void rejectsNullRequiredColumns(int nullColumnIndex) {
        Long caseId = createCase();
        Object[] values = validDatabaseValues(caseId);
        values[nullColumnIndex] = null;

        assertThatThrownBy(() -> insertDocument(values))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativeFileSize() {
        Object[] values = validDatabaseValues(createCase());
        values[6] = -1L;

        assertThatThrownBy(() -> insertDocument(values))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsUnsupportedDocumentSource() {
        Object[] values = validDatabaseValues(createCase());
        values[3] = "EXTERNAL";

        assertThatThrownBy(() -> insertDocument(values))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsUnsupportedFileFormat() {
        Object[] values = validDatabaseValues(createCase());
        values[4] = "PNG";

        assertThatThrownBy(() -> insertDocument(values))
                .isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @EnumSource(DocumentSource.class)
    void persistsEverySupportedDocumentSourceAsString(DocumentSource source) {
        CaseDocumentEntity saved = documentRepository.saveAndFlush(
                document(createCase(), source, DocumentFormat.PDF)
        );

        String storedSource = jdbcTemplate.queryForObject(
                "SELECT document_source FROM case_documents WHERE id = ?",
                String.class,
                saved.getId()
        );

        assertThat(storedSource).isEqualTo(source.name());
    }

    @ParameterizedTest
    @EnumSource(DocumentFormat.class)
    void persistsEverySupportedFileFormatAsString(DocumentFormat format) {
        CaseDocumentEntity saved = documentRepository.saveAndFlush(
                document(createCase(), DocumentSource.UPLOADED, format)
        );

        String storedFormat = jdbcTemplate.queryForObject(
                "SELECT file_format FROM case_documents WHERE id = ?",
                String.class,
                saved.getId()
        );

        assertThat(storedFormat).isEqualTo(format.name());
    }

    private Long createCase() {
        CaseEntity caseEntity = new CaseEntity(
                "DOC-TEST-" + UUID.randomUUID(),
                "Document metadata test case",
                CaseStatus.PENDING_FILING,
                "Test plaintiff",
                "Test defendant",
                "Test lawyer"
        );
        return caseRepository.saveAndFlush(caseEntity).getId();
    }

    private CaseDocumentEntity document(
            Long caseId,
            DocumentSource source,
            DocumentFormat format
    ) {
        return new CaseDocumentEntity(
                caseId,
                "证据材料.pdf",
                uniqueStorageKey(),
                source,
                format,
                "application/pdf",
                1024
        );
    }

    private Object[] validDatabaseValues(Long caseId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return new Object[]{
                caseId,
                "证据材料.pdf",
                uniqueStorageKey(),
                "UPLOADED",
                "PDF",
                "application/pdf",
                1024L,
                now,
                now
        };
    }

    private void insertDocument(Object[] values) {
        jdbcTemplate.update(
                """
                INSERT INTO case_documents (
                    case_id,
                    original_file_name,
                    storage_key,
                    document_source,
                    file_format,
                    content_type,
                    file_size,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                values
        );
    }

    private String uniqueStorageKey() {
        return "documents/" + UUID.randomUUID();
    }
}
