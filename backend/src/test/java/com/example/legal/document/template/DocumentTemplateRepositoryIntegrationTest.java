package com.example.legal.document.template;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentTemplateRepositoryIntegrationTest {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String VALID_SHA256 = "a".repeat(64);

    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateVersionRepository versionRepository;
    private final DocumentTemplateFieldRepository fieldRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    DocumentTemplateRepositoryIntegrationTest(
            DocumentTemplateRepository templateRepository,
            DocumentTemplateVersionRepository versionRepository,
            DocumentTemplateFieldRepository fieldRepository,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate
    ) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.fieldRepository = fieldRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void persistsTemplateVersionAndOrderedFieldContract() {
        DocumentTemplateEntity template = templateRepository.saveAndFlush(new DocumentTemplateEntity(
                "民事起诉状",
                "民事案件起诉文书",
                DocumentTemplateType.PRESET
        ));
        DocumentTemplateVersionEntity version = versionRepository.saveAndFlush(version(template.getId(), 1));
        fieldRepository.save(field(
                version.getId(),
                "案号",
                "案号",
                DocumentFieldValueType.TEXT,
                true,
                DocumentFieldDefaultSource.CASE_FIELD,
                "caseNumber",
                1
        ));
        fieldRepository.save(field(
                version.getId(),
                "当前日期",
                "当前日期",
                DocumentFieldValueType.DATE,
                false,
                DocumentFieldDefaultSource.SYSTEM_VALUE,
                "currentDate",
                0
        ));
        fieldRepository.flush();
        Long templateId = template.getId();
        Long versionId = version.getId();
        entityManager.clear();

        DocumentTemplateEntity retrievedTemplate = templateRepository.findById(templateId).orElseThrow();
        DocumentTemplateVersionEntity retrievedVersion = versionRepository
                .findByIdAndTemplateId(versionId, templateId)
                .orElseThrow();
        List<DocumentTemplateFieldEntity> fields = fieldRepository
                .findAllByTemplateVersionIdOrderByDisplayOrder(versionId);

        assertThat(retrievedTemplate.getName()).isEqualTo("民事起诉状");
        assertThat(retrievedTemplate.getDescription()).isEqualTo("民事案件起诉文书");
        assertThat(retrievedTemplate.getTemplateType()).isEqualTo(DocumentTemplateType.PRESET);
        assertThat(retrievedTemplate.getCreatedAt()).isNotNull();
        assertThat(retrievedTemplate.getUpdatedAt()).isNotNull();

        assertThat(retrievedVersion.getTemplateId()).isEqualTo(templateId);
        assertThat(retrievedVersion.getVersionNumber()).isEqualTo(1);
        assertThat(retrievedVersion.getOriginalFileName()).isEqualTo("民事起诉状.docx");
        assertThat(retrievedVersion.getContentType()).isEqualTo(DOCX_CONTENT_TYPE);
        assertThat(retrievedVersion.getFileSize()).isEqualTo(2048);
        assertThat(retrievedVersion.getContentSha256()).isEqualTo(VALID_SHA256);
        assertThat(retrievedVersion.getPublishedAt()).isNotNull();

        assertThat(fields).extracting(DocumentTemplateFieldEntity::getFieldKey)
                .containsExactly("当前日期", "案号");
        assertThat(fields.getFirst().getDisplayName()).isEqualTo("当前日期");
        assertThat(fields.getFirst().getDescription()).isEqualTo("模板字段说明");
        assertThat(fields.getFirst().getValueType()).isEqualTo(DocumentFieldValueType.DATE);
        assertThat(fields.getFirst().isRequired()).isFalse();
        assertThat(fields.getFirst().getDefaultSource()).isEqualTo(DocumentFieldDefaultSource.SYSTEM_VALUE);
        assertThat(fields.getFirst().getSourceKey()).isEqualTo("currentDate");
        assertThat(fields.getFirst().getDisplayOrder()).isZero();
        assertThat(fieldRepository.findByTemplateVersionIdAndFieldKey(versionId, "案号")).isPresent();
    }

    @Test
    void ordersMultipleVersionsNewestFirst() {
        Long templateId = createTemplate();
        versionRepository.save(version(templateId, 1));
        versionRepository.save(version(templateId, 2));
        versionRepository.flush();

        assertThat(versionRepository.findAllByTemplateIdOrderByVersionNumberDesc(templateId))
                .extracting(DocumentTemplateVersionEntity::getVersionNumber)
                .containsExactly(2, 1);
    }

    @Test
    void permitsSameVersionNumberAndFieldKeyInDifferentParents() {
        Long firstTemplateId = createTemplate();
        Long secondTemplateId = createTemplate();
        DocumentTemplateVersionEntity firstVersion = versionRepository.saveAndFlush(version(firstTemplateId, 1));
        DocumentTemplateVersionEntity secondVersion = versionRepository.saveAndFlush(version(secondTemplateId, 1));

        fieldRepository.saveAndFlush(userInputField(firstVersion.getId(), "诉讼请求", 0));
        fieldRepository.saveAndFlush(userInputField(secondVersion.getId(), "诉讼请求", 0));

        assertThat(fieldRepository.findByTemplateVersionIdAndFieldKey(firstVersion.getId(), "诉讼请求"))
                .isPresent();
        assertThat(fieldRepository.findByTemplateVersionIdAndFieldKey(secondVersion.getId(), "诉讼请求"))
                .isPresent();
    }

    @Test
    void permitsIdenticalContentDigestAcrossVersions() {
        Long templateId = createTemplate();

        versionRepository.saveAndFlush(version(templateId, 1));
        versionRepository.saveAndFlush(version(templateId, 2));

        assertThat(versionRepository.findAllByTemplateIdOrderByVersionNumberDesc(templateId))
                .extracting(DocumentTemplateVersionEntity::getContentSha256)
                .containsExactly(VALID_SHA256, VALID_SHA256);
    }

    @Test
    void permitsZeroFileSizeAndNullableDescriptions() {
        DocumentTemplateEntity template = templateRepository.saveAndFlush(new DocumentTemplateEntity(
                "无描述模板",
                null,
                DocumentTemplateType.CUSTOM
        ));
        insertVersion(template.getId(), 1, 0, VALID_SHA256);
        Long versionId = jdbcTemplate.queryForObject(
                "SELECT id FROM document_template_versions WHERE template_id = ? AND version_number = 1",
                Long.class,
                template.getId()
        );
        DocumentTemplateFieldEntity field = fieldRepository.saveAndFlush(new DocumentTemplateFieldEntity(
                versionId,
                "备注",
                "备注",
                null,
                DocumentFieldValueType.TEXT,
                false,
                DocumentFieldDefaultSource.USER_INPUT,
                null,
                0
        ));

        assertThat(template.getDescription()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT file_size FROM document_template_versions WHERE id = ?",
                Long.class,
                versionId
        )).isZero();
        assertThat(field.getDescription()).isNull();
        assertThat(field.isRequired()).isFalse();
    }

    @Test
    void returnsEmptyResultsForMissingAndCrossParentLookups() {
        Long firstTemplateId = createTemplate();
        Long secondTemplateId = createTemplate();
        DocumentTemplateVersionEntity firstVersion = versionRepository.saveAndFlush(version(firstTemplateId, 1));
        DocumentTemplateVersionEntity secondVersion = versionRepository.saveAndFlush(version(secondTemplateId, 1));
        fieldRepository.saveAndFlush(userInputField(firstVersion.getId(), "诉讼请求", 0));

        assertThat(versionRepository.findByIdAndTemplateId(firstVersion.getId(), secondTemplateId)).isEmpty();
        assertThat(fieldRepository.findByTemplateVersionIdAndFieldKey(secondVersion.getId(), "诉讼请求"))
                .isEmpty();
        assertThat(versionRepository.findAllByTemplateIdOrderByVersionNumberDesc(Long.MAX_VALUE)).isEmpty();
        assertThat(fieldRepository.findAllByTemplateVersionIdOrderByDisplayOrder(secondVersion.getId())).isEmpty();
    }

    @Test
    void rejectsDuplicateVersionNumberWithinTemplate() {
        Long templateId = createTemplate();
        versionRepository.saveAndFlush(version(templateId, 1));

        assertThatThrownBy(() -> versionRepository.saveAndFlush(version(templateId, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateStorageKey() {
        Long templateId = createTemplate();
        String storageKey = uniqueStorageKey();
        versionRepository.saveAndFlush(version(templateId, 1, storageKey));

        assertThatThrownBy(() -> versionRepository.saveAndFlush(version(templateId, 2, storageKey)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateFieldKeyWithinVersion() {
        Long versionId = createVersion();
        fieldRepository.saveAndFlush(userInputField(versionId, "诉讼请求", 0));

        assertThatThrownBy(() -> fieldRepository.saveAndFlush(userInputField(versionId, "诉讼请求", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateDisplayOrderWithinVersion() {
        Long versionId = createVersion();
        fieldRepository.saveAndFlush(userInputField(versionId, "诉讼请求", 0));

        assertThatThrownBy(() -> fieldRepository.saveAndFlush(userInputField(versionId, "事实与理由", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingTemplateReferenceForVersion() {
        assertThatThrownBy(() -> versionRepository.saveAndFlush(version(Long.MAX_VALUE, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingVersionReferenceForField() {
        assertThatThrownBy(() -> fieldRepository.saveAndFlush(userInputField(Long.MAX_VALUE, "诉讼请求", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void restrictsDeletingTemplateWithPublishedVersion() {
        Long templateId = createTemplate();
        versionRepository.saveAndFlush(version(templateId, 1));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM document_templates WHERE id = ?",
                templateId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void restrictsDeletingVersionWithFieldContract() {
        Long versionId = createVersion();
        fieldRepository.saveAndFlush(userInputField(versionId, "诉讼请求", 0));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM document_template_versions WHERE id = ?",
                versionId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @EnumSource(DocumentTemplateType.class)
    void persistsEveryTemplateTypeAsString(DocumentTemplateType type) {
        DocumentTemplateEntity saved = templateRepository.saveAndFlush(
                new DocumentTemplateEntity("Template " + UUID.randomUUID(), null, type)
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT template_type FROM document_templates WHERE id = ?",
                String.class,
                saved.getId()
        )).isEqualTo(type.name());
    }

    @ParameterizedTest
    @EnumSource(DocumentFieldValueType.class)
    void persistsEveryFieldValueTypeAsString(DocumentFieldValueType valueType) {
        DocumentTemplateFieldEntity saved = fieldRepository.saveAndFlush(field(
                createVersion(),
                "field-" + valueType.name(),
                valueType.name(),
                valueType,
                true,
                DocumentFieldDefaultSource.USER_INPUT,
                null,
                0
        ));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT value_type FROM document_template_fields WHERE id = ?",
                String.class,
                saved.getId()
        )).isEqualTo(valueType.name());
    }

    @ParameterizedTest
    @EnumSource(DocumentFieldDefaultSource.class)
    void persistsEveryDefaultSourceAsString(DocumentFieldDefaultSource source) {
        String sourceKey = source == DocumentFieldDefaultSource.USER_INPUT ? null : "approvedSource";
        DocumentTemplateFieldEntity saved = fieldRepository.saveAndFlush(field(
                createVersion(),
                "field-" + source.name(),
                source.name(),
                DocumentFieldValueType.TEXT,
                true,
                source,
                sourceKey,
                0
        ));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT default_source FROM document_template_fields WHERE id = ?",
                String.class,
                saved.getId()
        )).isEqualTo(source.name());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void rejectsNullRequiredTemplateColumns(int nullColumnIndex) {
        Object[] values = {
                "Template " + UUID.randomUUID(),
                "CUSTOM",
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
        };
        values[nullColumnIndex] = null;

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO document_templates (name, template_type, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                """,
                values
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void rejectsNullRequiredVersionColumns(int nullColumnIndex) {
        Object[] values = validVersionDatabaseValues(createTemplate());
        values[nullColumnIndex] = null;

        assertThatThrownBy(() -> insertVersion(values))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 7})
    void rejectsNullRequiredFieldColumns(int nullColumnIndex) {
        Object[] values = validFieldDatabaseValues(createVersion());
        values[nullColumnIndex] = null;

        assertThatThrownBy(() -> insertField(values))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankTemplateName(String name) {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO document_templates (name, template_type, created_at, updated_at)
                VALUES (?, 'CUSTOM', ?, ?)
                """,
                name,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
        )).isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void rejectsBlankRequiredVersionText(int blankColumnIndex) {
        Object[] values = validVersionDatabaseValues(createTemplate());
        values[blankColumnIndex] = "   ";

        assertThatThrownBy(() -> insertVersion(values))
                .isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void rejectsBlankRequiredFieldText(int blankColumnIndex) {
        Object[] values = validFieldDatabaseValues(createVersion());
        values[blankColumnIndex] = "   ";

        assertThatThrownBy(() -> insertField(values))
                .isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBoundSourceWithBlankSourceKey(String sourceKey) {
        assertThatThrownBy(() -> insertField(createVersion(), "CASE_FIELD", sourceKey, 0))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsUnsupportedTemplateType() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO document_templates (name, template_type, created_at, updated_at)
                VALUES ('Unsupported template', 'EXTERNAL', ?, ?)
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsUnsupportedFieldValueType() {
        Object[] values = validFieldDatabaseValues(createVersion());
        values[3] = "COLLECTION";

        assertThatThrownBy(() -> insertField(values))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsUnsupportedDefaultSource() {
        Object[] values = validFieldDatabaseValues(createVersion());
        values[5] = "AI";

        assertThatThrownBy(() -> insertField(values))
                .isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASE_FIELD", "SYSTEM_VALUE"})
    void rejectsBoundSourceWithoutSourceKey(String defaultSource) {
        assertThatThrownBy(() -> insertField(createVersion(), defaultSource, null, 0))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsUserInputWithSourceKey() {
        assertThatThrownBy(() -> insertField(createVersion(), "USER_INPUT", "unexpected", 0))
                .isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveVersionNumber(int versionNumber) {
        assertThatThrownBy(() -> insertVersion(createTemplate(), versionNumber, 10, VALID_SHA256))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsNegativeFileSize() {
        assertThatThrownBy(() -> insertVersion(createTemplate(), 1, -1, VALID_SHA256))
                .isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void rejectsInvalidSha256(String digest) {
        assertThatThrownBy(() -> insertVersion(createTemplate(), 1, 10, digest))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsExactLengthNonHexSha256() {
        assertThatThrownBy(() -> insertVersion(createTemplate(), 1, 10, "g".repeat(64)))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void acceptsLowercaseSha256() {
        assertThatCode(() -> insertVersion(createTemplate(), 1, 10, VALID_SHA256))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNegativeDisplayOrder() {
        assertThatThrownBy(() -> insertField(createVersion(), "USER_INPUT", null, -1))
                .isInstanceOf(DataAccessException.class);
    }

    private Long createTemplate() {
        return templateRepository.saveAndFlush(new DocumentTemplateEntity(
                "Template " + UUID.randomUUID(),
                null,
                DocumentTemplateType.CUSTOM
        )).getId();
    }

    private Long createVersion() {
        return versionRepository.saveAndFlush(version(createTemplate(), 1)).getId();
    }

    private DocumentTemplateVersionEntity version(Long templateId, int versionNumber) {
        return version(templateId, versionNumber, uniqueStorageKey());
    }

    private DocumentTemplateVersionEntity version(Long templateId, int versionNumber, String storageKey) {
        return new DocumentTemplateVersionEntity(
                templateId,
                versionNumber,
                "民事起诉状.docx",
                storageKey,
                DOCX_CONTENT_TYPE,
                2048,
                VALID_SHA256
        );
    }

    private DocumentTemplateFieldEntity userInputField(Long versionId, String key, int order) {
        return field(
                versionId,
                key,
                key,
                DocumentFieldValueType.TEXT,
                true,
                DocumentFieldDefaultSource.USER_INPUT,
                null,
                order
        );
    }

    private DocumentTemplateFieldEntity field(
            Long versionId,
            String key,
            String displayName,
            DocumentFieldValueType valueType,
            boolean required,
            DocumentFieldDefaultSource source,
            String sourceKey,
            int order
    ) {
        return new DocumentTemplateFieldEntity(
                versionId,
                key,
                displayName,
                "模板字段说明",
                valueType,
                required,
                source,
                sourceKey,
                order
        );
    }

    private void insertVersion(Long templateId, int versionNumber, long fileSize, String sha256) {
        Object[] values = validVersionDatabaseValues(templateId);
        values[1] = versionNumber;
        values[5] = fileSize;
        values[6] = sha256;
        insertVersion(values);
    }

    private Object[] validVersionDatabaseValues(Long templateId) {
        return new Object[]{
                templateId,
                1,
                "template.docx",
                uniqueStorageKey(),
                DOCX_CONTENT_TYPE,
                10L,
                VALID_SHA256,
                Timestamp.valueOf(LocalDateTime.now())
        };
    }

    private void insertVersion(Object[] values) {
        jdbcTemplate.update(
                """
                INSERT INTO document_template_versions (
                    template_id,
                    version_number,
                    original_file_name,
                    storage_key,
                    content_type,
                    file_size,
                    content_sha256,
                    published_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                values
        );
    }

    private void insertField(Long versionId, String defaultSource, String sourceKey, int displayOrder) {
        Object[] values = validFieldDatabaseValues(versionId);
        values[5] = defaultSource;
        values[6] = sourceKey;
        values[7] = displayOrder;
        insertField(values);
    }

    private Object[] validFieldDatabaseValues(Long versionId) {
        return new Object[]{
                versionId,
                "field-" + UUID.randomUUID(),
                "Test field",
                "TEXT",
                true,
                "USER_INPUT",
                null,
                0
        };
    }

    private void insertField(Object[] values) {
        jdbcTemplate.update(
                """
                INSERT INTO document_template_fields (
                    template_version_id,
                    field_key,
                    display_name,
                    value_type,
                    required,
                    default_source,
                    source_key,
                    display_order
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                values
        );
    }

    private String uniqueStorageKey() {
        return "templates/" + UUID.randomUUID();
    }
}
