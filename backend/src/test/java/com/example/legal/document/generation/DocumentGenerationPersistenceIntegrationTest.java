package com.example.legal.document.generation;

import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.read.CaseDocumentQueryService;
import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.DocumentTemplateEntity;
import com.example.legal.document.template.DocumentTemplateFieldEntity;
import com.example.legal.document.template.DocumentTemplateFieldRepository;
import com.example.legal.document.template.DocumentTemplateRepository;
import com.example.legal.document.template.DocumentTemplateType;
import com.example.legal.document.template.DocumentTemplateVersionEntity;
import com.example.legal.document.template.DocumentTemplateVersionRepository;
import com.example.legal.legalcase.CaseEntity;
import com.example.legal.legalcase.CaseRepository;
import com.example.legal.legalcase.CaseStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DocumentGenerationPersistenceIntegrationTest {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String TEMPLATE_SHA256 = "a".repeat(64);

    private final DocumentGenerationPersistenceService persistenceService;
    private final DocumentGenerationRepository generationRepository;
    private final GenerationValueRepository valueRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseRepository caseRepository;
    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateVersionRepository versionRepository;
    private final DocumentTemplateFieldRepository fieldRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final CaseDocumentQueryService caseDocumentQueryService;

    @Autowired
    DocumentGenerationPersistenceIntegrationTest(
            DocumentGenerationPersistenceService persistenceService,
            DocumentGenerationRepository generationRepository,
            GenerationValueRepository valueRepository,
            CaseDocumentRepository caseDocumentRepository,
            CaseRepository caseRepository,
            DocumentTemplateRepository templateRepository,
            DocumentTemplateVersionRepository versionRepository,
            DocumentTemplateFieldRepository fieldRepository,
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager,
            CaseDocumentQueryService caseDocumentQueryService
    ) {
        this.persistenceService = persistenceService;
        this.generationRepository = generationRepository;
        this.valueRepository = valueRepository;
        this.caseDocumentRepository = caseDocumentRepository;
        this.caseRepository = caseRepository;
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.fieldRepository = fieldRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.caseDocumentQueryService = caseDocumentQueryService;
    }

    @BeforeEach
    @AfterEach
    void cleanGenerationPersistenceFixtures() {
        jdbcTemplate.update("""
                DELETE gv FROM generation_values gv
                JOIN document_generations dg ON dg.id = gv.generation_id
                JOIN cases c ON c.id = dg.case_id
                WHERE c.case_number LIKE 'GEN-PERSIST-%'
                   OR c.case_name = 'Generation persistence case'
                """);
        jdbcTemplate.update("""
                DELETE dg FROM document_generations dg
                JOIN cases c ON c.id = dg.case_id
                WHERE c.case_number LIKE 'GEN-PERSIST-%'
                   OR c.case_name = 'Generation persistence case'
                """);
        jdbcTemplate.update("""
                DELETE cd FROM case_documents cd
                JOIN cases c ON c.id = cd.case_id
                WHERE c.case_number LIKE 'GEN-PERSIST-%'
                   OR c.case_name = 'Generation persistence case'
                """);
        jdbcTemplate.update("""
                DELETE dtf FROM document_template_fields dtf
                JOIN document_template_versions dtv ON dtv.id = dtf.template_version_id
                JOIN document_templates dt ON dt.id = dtv.template_id
                WHERE dt.name LIKE 'Generation persistence template %'
                   OR dt.name LIKE 'Generation template %'
                """);
        jdbcTemplate.update("""
                DELETE dtv FROM document_template_versions dtv
                JOIN document_templates dt ON dt.id = dtv.template_id
                WHERE dt.name LIKE 'Generation persistence template %'
                   OR dt.name LIKE 'Generation template %'
                """);
        jdbcTemplate.update(
                """
                DELETE FROM document_templates
                WHERE name LIKE 'Generation persistence template %'
                   OR name LIKE 'Generation template %'
                """
        );
        jdbcTemplate.update("""
                DELETE FROM cases
                WHERE case_number LIKE 'GEN-PERSIST-%'
                   OR case_name = 'Generation persistence case'
                """);
    }

    @Test
    void persistsGeneratedDocumentGenerationAndExactReviewedValues() {
        Fixture fixture = fixture();
        String idempotencyKey = UUID.randomUUID().toString();
        String storageKey = storageKey();

        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                idempotencyKey,
                storageKey,
                List.of(
                        new GenerationValueToPersist(
                                fixture.caseNumberFieldId(),
                                "  (2026)沪0115民初1001号  ",
                                GenerationValueSource.CASE_FIELD
                        ),
                        new GenerationValueToPersist(
                                fixture.currentDateFieldId(),
                                "2026年08月05日",
                                GenerationValueSource.SYSTEM_VALUE
                        )
                )
        ));

        entityManager.clear();
        DocumentGenerationEntity generation = generationRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow();
        List<GenerationValueEntity> values = valueRepository
                .findAllByGenerationIdOrderById(generation.getId());

        assertThat(persisted.caseDocument().getDocumentSource().name()).isEqualTo("GENERATED");
        assertThat(persisted.caseDocument().getFileFormat().name()).isEqualTo("DOCX");
        assertThat(generation.getCaseId()).isEqualTo(fixture.caseId());
        assertThat(generation.getTemplateVersionId()).isEqualTo(fixture.versionId());
        assertThat(generation.getCaseDocumentId()).isEqualTo(persisted.caseDocument().getId());
        assertThat(generation.getCaseStatusSnapshot()).isEqualTo(CaseStatus.IN_TRIAL);
        assertThat(generation.getRequestSha256()).isEqualTo("b".repeat(64));
        assertThat(generation.getCreatedAt()).isNotNull();
        assertThat(caseDocumentQueryService.getCaseDocuments(fixture.caseId()).data())
                .filteredOn(document -> document.id().equals(generation.getCaseDocumentId()))
                .singleElement()
                .satisfies(document -> assertThat(document.generatedAt())
                        .isEqualTo(generation.getCreatedAt().toInstant(ZoneOffset.UTC)));
        assertThat(values).extracting(GenerationValueEntity::getResolvedValue)
                .containsExactly("  (2026)沪0115民初1001号  ", "2026年08月05日");
        assertThat(values).extracting(GenerationValueEntity::getValueSource)
                .containsExactly(GenerationValueSource.CASE_FIELD, GenerationValueSource.SYSTEM_VALUE);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT storage_key FROM case_documents WHERE id = ?",
                String.class,
                generation.getCaseDocumentId()
        )).isEqualTo(storageKey);
    }

    @Test
    void permitsGenerationForAnEmptyTemplateContract() {
        Fixture fixture = fixtureWithoutFields();

        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                storageKey(),
                List.of()
        ));

        assertThat(valueRepository.findAllByGenerationIdOrderById(persisted.generation().getId()))
                .isEmpty();
    }

    @Test
    void duplicateIdempotencyKeyRollsBackTheLosingDocumentMetadata() {
        Fixture fixture = fixture();
        String idempotencyKey = UUID.randomUUID().toString();
        persistenceService.persist(command(
                fixture,
                idempotencyKey,
                storageKey(),
                validValues(fixture)
        ));
        String losingStorageKey = storageKey();

        assertThatThrownBy(() -> persistenceService.persist(command(
                fixture,
                idempotencyKey,
                losingStorageKey,
                validValues(fixture)
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM case_documents WHERE storage_key = ?",
                Long.class,
                losingStorageKey
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document_generations WHERE idempotency_key = ?",
                Long.class,
                idempotencyKey
        )).isOne();
    }

    @Test
    void rejectsAFieldOwnedByAnotherTemplateVersionBeforeWritingMetadata() {
        Fixture selected = fixtureWithoutFields();
        Fixture other = fixture();
        String rejectedStorageKey = storageKey();

        assertThatThrownBy(() -> persistenceService.persist(command(
                selected,
                UUID.randomUUID().toString(),
                rejectedStorageKey,
                List.of(new GenerationValueToPersist(
                        other.caseNumberFieldId(),
                        "value",
                        GenerationValueSource.USER_INPUT
                ))
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selected template version");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM case_documents WHERE storage_key = ?",
                Long.class,
                rejectedStorageKey
        )).isZero();
    }

    @Test
    void rejectsDuplicateTemplateFieldBeforeWritingMetadata() {
        Fixture fixture = fixture();
        String rejectedStorageKey = storageKey();
        GenerationValueToPersist value = new GenerationValueToPersist(
                fixture.caseNumberFieldId(),
                "value",
                GenerationValueSource.USER_INPUT
        );

        assertThatThrownBy(() -> persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                rejectedStorageKey,
                List.of(value, value)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not repeat");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM case_documents WHERE storage_key = ?",
                Long.class,
                rejectedStorageKey
        )).isZero();
    }

    @Test
    void rejectsIncompleteFieldContractBeforeWritingMetadata() {
        Fixture fixture = fixture();
        String rejectedStorageKey = storageKey();

        assertThatThrownBy(() -> persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                rejectedStorageKey,
                List.of(new GenerationValueToPersist(
                        fixture.caseNumberFieldId(),
                        "value",
                        GenerationValueSource.USER_INPUT
                ))
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cover");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM case_documents WHERE storage_key = ?",
                Long.class,
                rejectedStorageKey
        )).isZero();
    }

    @Test
    void removingGeneratedCaseDocumentRetainsGenerationAndValues() {
        Fixture fixture = fixture();
        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                storageKey(),
                validValues(fixture)
        ));
        Long generationId = persisted.generation().getId();

        caseDocumentRepository.deleteById(persisted.caseDocument().getId());
        caseDocumentRepository.flush();
        entityManager.clear();

        DocumentGenerationEntity retained = generationRepository.findById(generationId).orElseThrow();
        assertThat(retained.getCaseDocumentId()).isNull();
        assertThat(valueRepository.findAllByGenerationIdOrderById(generationId)).hasSize(2);
    }

    @Test
    void restrictiveReferencesProtectGenerationHistory() {
        Fixture fixture = fixture();
        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                storageKey(),
                validValues(fixture)
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM cases WHERE id = ?",
                fixture.caseId()
        )).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM document_template_versions WHERE id = ?",
                fixture.versionId()
        )).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM document_template_fields WHERE id = ?",
                fixture.caseNumberFieldId()
        )).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM document_generations WHERE id = ?",
                persisted.generation().getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidGenerationValueSource() {
        Fixture fixture = fixture();
        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                storageKey(),
                validValues(fixture)
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO generation_values (
                    generation_id, template_field_id, resolved_value, value_source
                ) VALUES (?, ?, ?, ?)
                """,
                persisted.generation().getId(),
                fixture.caseNumberFieldId(),
                "value",
                "INFERRED"
        )).isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @EnumSource(GenerationValueSource.class)
    void persistsEveryGenerationValueSourceAsString(GenerationValueSource source) {
        Fixture fixture = fixture();
        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                storageKey(),
                List.of(
                        new GenerationValueToPersist(fixture.caseNumberFieldId(), "one", source),
                        new GenerationValueToPersist(fixture.currentDateFieldId(), "two", source)
                )
        ));

        assertThat(jdbcTemplate.queryForList(
                "SELECT value_source FROM generation_values WHERE generation_id = ? ORDER BY id",
                String.class,
                persisted.generation().getId()
        )).containsExactly(source.name(), source.name());
    }

    @Test
    void databaseRejectsMalformedIdempotencyKeyAndRequestDigest() {
        Fixture fixture = fixture();
        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                storageKey(),
                validValues(fixture)
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE document_generations SET idempotency_key = ? WHERE id = ?",
                "not-a-uuid",
                persisted.generation().getId()
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE document_generations SET request_sha256 = ? WHERE id = ?",
                "A".repeat(64),
                persisted.generation().getId()
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void databaseRejectsReusingOneCaseDocumentForTwoGenerations() {
        Fixture fixture = fixture();
        PersistedDocumentGeneration persisted = persistenceService.persist(command(
                fixture,
                UUID.randomUUID().toString(),
                storageKey(),
                validValues(fixture)
        ));

        assertThatThrownBy(() -> generationRepository.saveAndFlush(new DocumentGenerationEntity(
                fixture.caseId(),
                fixture.versionId(),
                persisted.caseDocument().getId(),
                CaseStatus.IN_TRIAL,
                UUID.randomUUID().toString(),
                "c".repeat(64),
                LocalDateTime.of(2026, 8, 7, 0, 0)
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Fixture fixture() {
        return fixture(true);
    }

    private Fixture fixtureWithoutFields() {
        return fixture(false);
    }

    private Fixture fixture(boolean withFields) {
        CaseEntity caseEntity = caseRepository.saveAndFlush(new CaseEntity(
                "GEN-PERSIST-" + UUID.randomUUID(),
                "Generation persistence case",
                CaseStatus.IN_TRIAL,
                "Plaintiff",
                "Defendant",
                "Lawyer"
        ));
        DocumentTemplateEntity template = templateRepository.saveAndFlush(new DocumentTemplateEntity(
                "Generation persistence template " + UUID.randomUUID(),
                null,
                DocumentTemplateType.CUSTOM
        ));
        DocumentTemplateVersionEntity version = versionRepository.saveAndFlush(
                new DocumentTemplateVersionEntity(
                        template.getId(),
                        1,
                        "template.docx",
                        storageKey(),
                        DOCX_CONTENT_TYPE,
                        128,
                        TEMPLATE_SHA256
                )
        );
        if (!withFields) {
            return new Fixture(caseEntity.getId(), version.getId(), null, null);
        }
        DocumentTemplateFieldEntity caseNumber = fieldRepository.save(new DocumentTemplateFieldEntity(
                version.getId(),
                "case_number",
                "案号",
                null,
                DocumentFieldValueType.TEXT,
                true,
                DocumentFieldDefaultSource.CASE_FIELD,
                "caseNumber",
                0
        ));
        DocumentTemplateFieldEntity currentDate = fieldRepository.save(new DocumentTemplateFieldEntity(
                version.getId(),
                "current_date",
                "当前日期",
                null,
                DocumentFieldValueType.DATE,
                true,
                DocumentFieldDefaultSource.SYSTEM_VALUE,
                "currentDate",
                1
        ));
        fieldRepository.flush();
        return new Fixture(caseEntity.getId(), version.getId(), caseNumber.getId(), currentDate.getId());
    }

    private GenerationPersistenceCommand command(
            Fixture fixture,
            String idempotencyKey,
            String storageKey,
            List<GenerationValueToPersist> values
    ) {
        return new GenerationPersistenceCommand(
                fixture.caseId(),
                fixture.versionId(),
                CaseStatus.IN_TRIAL,
                idempotencyKey,
                "b".repeat(64),
                "generated.docx",
                storageKey,
                DOCX_CONTENT_TYPE,
                256,
                values
        );
    }

    private List<GenerationValueToPersist> validValues(Fixture fixture) {
        return List.of(
                new GenerationValueToPersist(
                        fixture.caseNumberFieldId(),
                        "(2026)沪0115民初1001号",
                        GenerationValueSource.CASE_FIELD
                ),
                new GenerationValueToPersist(
                        fixture.currentDateFieldId(),
                        "2026年8月5日",
                        GenerationValueSource.SYSTEM_VALUE
                )
        );
    }

    private String storageKey() {
        return "generation-test/" + UUID.randomUUID();
    }

    private record Fixture(
            Long caseId,
            Long versionId,
            Long caseNumberFieldId,
            Long currentDateFieldId
    ) {
    }
}
