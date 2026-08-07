package com.example.legal.document.generation;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import com.example.legal.document.storage.StoredDocument;
import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.DocumentTemplateFieldEntity;
import com.example.legal.document.template.DocumentTemplateFieldRepository;
import com.example.legal.document.template.DocumentTemplateVersionEntity;
import com.example.legal.document.template.DocumentTemplateVersionRepository;
import com.example.legal.legalcase.CaseEntity;
import com.example.legal.legalcase.CaseRepository;
import com.example.legal.legalcase.CaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentGenerationServiceTest {

    private static final byte[] TEMPLATE = "template".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OUTPUT = "output".getBytes(StandardCharsets.UTF_8);
    private static final String TIMEZONE = "Asia/Shanghai";
    private static final LocalDateTime GENERATED_AT_UTC = LocalDateTime.of(2026, 8, 4, 16, 30);

    private CaseRepository caseRepository;
    private DocumentTemplateVersionRepository versionRepository;
    private DocumentTemplateFieldRepository fieldRepository;
    private DocumentGenerationRepository generationRepository;
    private CaseDocumentRepository caseDocumentRepository;
    private TemplateDocumentRenderer renderer;
    private DocumentStorage storage;
    private DocumentGenerationPersistenceService persistenceService;
    private DocumentGenerationService service;
    private CaseEntity legalCase;
    private DocumentTemplateVersionEntity version;
    private DocumentTemplateFieldEntity field;
    private AtomicReference<GenerationPersistenceCommand> persistedCommand;
    private MutableClock clock;

    @BeforeEach
    void setUp() throws Exception {
        caseRepository = mock(CaseRepository.class);
        versionRepository = mock(DocumentTemplateVersionRepository.class);
        fieldRepository = mock(DocumentTemplateFieldRepository.class);
        generationRepository = mock(DocumentGenerationRepository.class);
        caseDocumentRepository = mock(CaseDocumentRepository.class);
        renderer = mock(TemplateDocumentRenderer.class);
        storage = mock(DocumentStorage.class);
        persistenceService = mock(DocumentGenerationPersistenceService.class);

        clock = new MutableClock(Instant.parse("2026-08-04T16:30:00Z"));
        GenerationValueResolver resolver = new GenerationValueResolver(clock);
        service = new DocumentGenerationService(
                caseRepository, versionRepository, fieldRepository, generationRepository,
                caseDocumentRepository, resolver, new GenerationValueValidator(resolver),
                renderer, storage, persistenceService
        );

        legalCase = new CaseEntity(
                "(2026)沪0115民初1001号", "合同纠纷", CaseStatus.IN_TRIAL,
                "张三", "某公司", "李律师"
        );
        ReflectionTestUtils.setField(legalCase, "id", 7L);
        version = new DocumentTemplateVersionEntity(
                11L, 2, "template.docx", "template-key",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                TEMPLATE.length, sha256(TEMPLATE)
        );
        ReflectionTestUtils.setField(version, "id", 22L);
        field = new DocumentTemplateFieldEntity(
                22L, "case_number", "案号", null, DocumentFieldValueType.TEXT,
                true, DocumentFieldDefaultSource.CASE_FIELD, "caseNumber", 0
        );
        ReflectionTestUtils.setField(field, "id", 33L);

        when(caseRepository.findById(7L)).thenReturn(Optional.of(legalCase));
        when(versionRepository.findByTemplateIdAndVersionNumber(11L, 2)).thenReturn(Optional.of(version));
        when(fieldRepository.findAllByTemplateVersionIdOrderByDisplayOrder(22L)).thenReturn(List.of(field));
        when(generationRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(storage.open("template-key")).thenAnswer(ignored -> new ByteArrayInputStream(TEMPLATE));
        when(renderer.render(any(), any())).thenReturn(OUTPUT);
        when(storage.store(any())).thenReturn(new StoredDocument("output-key", OUTPUT.length));

        persistedCommand = new AtomicReference<>();
        when(persistenceService.persist(any())).thenAnswer(invocation -> {
            GenerationPersistenceCommand command = invocation.getArgument(0);
            persistedCommand.set(command);
            CaseDocumentEntity document = new CaseDocumentEntity(
                    command.caseId(), command.fileName(), command.storageKey(), DocumentSource.GENERATED,
                    DocumentFormat.DOCX, command.contentType(), command.fileSize()
            );
            ReflectionTestUtils.setField(document, "id", 44L);
            DocumentGenerationEntity generation = new DocumentGenerationEntity(
                    command.caseId(), command.templateVersionId(), 44L, command.caseStatusSnapshot(),
                    command.idempotencyKey(), command.requestSha256(), GENERATED_AT_UTC
            );
            ReflectionTestUtils.setField(generation, "id", 55L);
            return new PersistedDocumentGeneration(generation, document, List.of());
        });
    }

    @Test
    void preparesTimezoneAwareSuggestionsWithoutPersisting() {
        GenerationPreparation prepared = service.prepare(7L, 11L, 2, TIMEZONE);

        assertThat(prepared.timezone()).isEqualTo(TIMEZONE);
        assertThat(prepared.fields()).singleElement().satisfies(preparedField -> {
            assertThat(preparedField.suggestedValue()).isEqualTo(legalCase.getCaseNumber());
            assertThat(preparedField.status()).isEqualTo(GenerationValueStatus.RESOLVED);
        });
        verify(persistenceService, never()).persist(any());
    }

    @Test
    void generatesStoresAndPersistsExactReviewedValue() {
        GeneratedDocument result = service.generate(command(
                UUID.randomUUID().toString(), legalCase.getCaseNumber(), GenerationValueSource.CASE_FIELD
        ));

        assertThat(result.generationId()).isEqualTo(55L);
        assertThat(result.caseDocumentId()).isEqualTo(44L);
        assertThat(result.outputAvailable()).isTrue();
        assertThat(persistedCommand.get().values()).singleElement().satisfies(value -> {
            assertThat(value.templateFieldId()).isEqualTo(33L);
            assertThat(value.resolvedValue()).isEqualTo(legalCase.getCaseNumber());
            assertThat(value.valueSource()).isEqualTo(GenerationValueSource.CASE_FIELD);
        });
        verify(renderer).render(TEMPLATE, Map.of("case_number", legalCase.getCaseNumber()));
    }

    @Test
    void sameIdempotencyKeyAndFingerprintReturnsExistingOutputBeforeTemplateAccess() {
        String key = UUID.randomUUID().toString();
        String originalCaseNumber = legalCase.getCaseNumber();
        service.generate(command(key, originalCaseNumber, GenerationValueSource.CASE_FIELD));
        GenerationPersistenceCommand first = persistedCommand.get();
        DocumentGenerationEntity existing = new DocumentGenerationEntity(
                7L, 22L, 44L, CaseStatus.IN_TRIAL, key, first.requestSha256(), GENERATED_AT_UTC
        );
        ReflectionTestUtils.setField(existing, "id", 55L);
        CaseDocumentEntity document = new CaseDocumentEntity(
                7L, "generated.docx", "existing-key", DocumentSource.GENERATED,
                DocumentFormat.DOCX, "application/test", 10
        );
        ReflectionTestUtils.setField(document, "id", 44L);
        when(generationRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));
        when(caseDocumentRepository.findById(44L)).thenReturn(Optional.of(document));
        legalCase.setCaseNumber("后来修改的案号");

        GeneratedDocument replay = service.generate(command(
                key, originalCaseNumber, GenerationValueSource.CASE_FIELD
        ));

        assertThat(replay.generationId()).isEqualTo(55L);
        assertThat(replay.outputAvailable()).isTrue();
        assertThat(replay.createdAt()).isEqualTo(GENERATED_AT_UTC.toInstant(ZoneOffset.UTC));
    }

    @Test
    void sameIdempotencyKeyReplaysAfterCurrentDateAdvances() {
        field = new DocumentTemplateFieldEntity(
                22L, "current_date", "当前日期", null, DocumentFieldValueType.DATE,
                true, DocumentFieldDefaultSource.SYSTEM_VALUE, "currentDate", 0
        );
        ReflectionTestUtils.setField(field, "id", 34L);
        when(fieldRepository.findAllByTemplateVersionIdOrderByDisplayOrder(22L)).thenReturn(List.of(field));
        String key = UUID.randomUUID().toString();
        DocumentGenerationCommand original = new DocumentGenerationCommand(
                7L, 11L, 2, TIMEZONE, key,
                List.of(new ReviewedGenerationValue(
                        "current_date", "2026年08月05日", GenerationValueSource.SYSTEM_VALUE
                ))
        );
        service.generate(original);
        GenerationPersistenceCommand first = persistedCommand.get();
        DocumentGenerationEntity existing = new DocumentGenerationEntity(
                7L, 22L, 44L, CaseStatus.IN_TRIAL, key, first.requestSha256(), GENERATED_AT_UTC
        );
        ReflectionTestUtils.setField(existing, "id", 56L);
        CaseDocumentEntity document = new CaseDocumentEntity(
                7L, "generated.docx", "existing-key", DocumentSource.GENERATED,
                DocumentFormat.DOCX, "application/test", 10
        );
        ReflectionTestUtils.setField(document, "id", 44L);
        when(generationRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));
        when(caseDocumentRepository.findById(44L)).thenReturn(Optional.of(document));
        clock.setInstant(Instant.parse("2026-08-05T16:30:00Z"));

        GeneratedDocument replay = service.generate(original);

        assertThat(replay.generationId()).isEqualTo(56L);
        assertThat(replay.outputAvailable()).isTrue();
        assertThat(replay.createdAt()).isEqualTo(GENERATED_AT_UTC.toInstant(ZoneOffset.UTC));
    }

    @Test
    void sameIdempotencyKeyWithDifferentRequestConflicts() {
        String key = UUID.randomUUID().toString();
        DocumentGenerationEntity existing = new DocumentGenerationEntity(
                7L, 22L, 44L, CaseStatus.IN_TRIAL, key, "a".repeat(64), GENERATED_AT_UTC
        );
        when(generationRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        assertFailure(
                () -> service.generate(command(key, "人工值", GenerationValueSource.USER_INPUT)),
                GenerationErrorCode.GENERATION_IDEMPOTENCY_CONFLICT,
                HttpStatus.CONFLICT
        );
        verify(storage, never()).store(any());
    }

    @Test
    void templateIntegrityMismatchFailsBeforeRenderingOrOutputStorage() {
        version = new DocumentTemplateVersionEntity(
                11L, 2, "template.docx", "template-key", "application/test",
                TEMPLATE.length, "a".repeat(64)
        );
        ReflectionTestUtils.setField(version, "id", 22L);
        when(versionRepository.findByTemplateIdAndVersionNumber(11L, 2)).thenReturn(Optional.of(version));

        assertFailure(
                () -> service.generate(command(
                        UUID.randomUUID().toString(), legalCase.getCaseNumber(), GenerationValueSource.CASE_FIELD
                )),
                GenerationErrorCode.GENERATION_TEMPLATE_INTEGRITY_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        verify(renderer, never()).render(any(), any());
        verify(storage, never()).store(any());
    }

    @Test
    void persistenceFailureCompensatesExclusiveOutputAndPreservesFailure() {
        RuntimeException databaseFailure = new RuntimeException("database unavailable");
        doThrow(databaseFailure).when(persistenceService).persist(any());

        assertFailure(
                () -> service.generate(command(
                        UUID.randomUUID().toString(), legalCase.getCaseNumber(), GenerationValueSource.CASE_FIELD
                )),
                GenerationErrorCode.GENERATION_PERSISTENCE_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        verify(storage).remove("output-key");
    }

    @Test
    void postRollbackMatchingWinnerReturnsExistingResultAfterCompensation() {
        String key = UUID.randomUUID().toString();
        doAnswer(invocation -> {
            GenerationPersistenceCommand command = invocation.getArgument(0);
            DocumentGenerationEntity winner = new DocumentGenerationEntity(
                    7L, 22L, 44L, CaseStatus.IN_TRIAL, key, command.requestSha256(), GENERATED_AT_UTC
            );
            ReflectionTestUtils.setField(winner, "id", 88L);
            when(generationRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(winner));
            throw new RuntimeException("unique constraint");
        }).when(persistenceService).persist(any());
        CaseDocumentEntity document = new CaseDocumentEntity(
                7L, "winner.docx", "winner-key", DocumentSource.GENERATED,
                DocumentFormat.DOCX, "application/test", 10
        );
        ReflectionTestUtils.setField(document, "id", 44L);
        when(caseDocumentRepository.findById(44L)).thenReturn(Optional.of(document));

        GeneratedDocument result = service.generate(command(
                key, legalCase.getCaseNumber(), GenerationValueSource.CASE_FIELD
        ));

        assertThat(result.generationId()).isEqualTo(88L);
        verify(storage).remove("output-key");
    }

    @Test
    void renderingAndStorageFailuresAreControlled() {
        doThrow(new TemplateRenderingException(
                TemplateRenderingErrorCode.TEMPLATE_RENDERING_TEMPLATE_UNSUPPORTED,
                "unsupported", Map.of()
        )).when(renderer).render(any(), any());
        assertFailure(
                () -> service.generate(command(
                        UUID.randomUUID().toString(), legalCase.getCaseNumber(), GenerationValueSource.CASE_FIELD
                )),
                GenerationErrorCode.GENERATION_RENDERING_FAILED,
                HttpStatus.UNPROCESSABLE_ENTITY
        );

        doReturn(OUTPUT).when(renderer).render(any(), any());
        when(storage.store(any())).thenThrow(new DocumentStorageException("unavailable"));
        assertFailure(
                () -> service.generate(command(
                        UUID.randomUUID().toString(), legalCase.getCaseNumber(), GenerationValueSource.CASE_FIELD
                )),
                GenerationErrorCode.GENERATION_STORAGE_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @Test
    void invalidTimezoneIdempotencyAndMissingResourcesAreControlled() {
        assertFailure(
                () -> service.prepare(7L, 11L, 2, "+08:00"),
                GenerationErrorCode.GENERATION_REQUEST_INVALID,
                HttpStatus.BAD_REQUEST
        );
        assertFailure(
                () -> service.generate(command(
                        "NOT-A-UUID", legalCase.getCaseNumber(), GenerationValueSource.CASE_FIELD
                )),
                GenerationErrorCode.GENERATION_REQUEST_INVALID,
                HttpStatus.BAD_REQUEST
        );
        assertFailure(
                () -> service.prepare(999L, 11L, 2, TIMEZONE),
                GenerationErrorCode.GENERATION_CASE_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
        assertFailure(
                () -> service.prepare(7L, 999L, 2, TIMEZONE),
                GenerationErrorCode.GENERATION_TEMPLATE_VERSION_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
    }

    private DocumentGenerationCommand command(String key, String value, GenerationValueSource source) {
        return new DocumentGenerationCommand(
                7L, 11L, 2, TIMEZONE, key,
                List.of(new ReviewedGenerationValue("case_number", value, source))
        );
    }

    private void assertFailure(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            GenerationErrorCode code,
            HttpStatus status
    ) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(DocumentGenerationException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(code);
            assertThat(exception.getStatus()).isEqualTo(status);
        });
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
