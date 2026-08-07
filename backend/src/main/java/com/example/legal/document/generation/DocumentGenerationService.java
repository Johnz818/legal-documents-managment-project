package com.example.legal.document.generation;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import com.example.legal.document.storage.DocumentStorageRequest;
import com.example.legal.document.storage.StoredDocument;
import com.example.legal.document.template.DocumentTemplateFieldEntity;
import com.example.legal.document.template.DocumentTemplateFieldRepository;
import com.example.legal.document.template.DocumentTemplateVersionEntity;
import com.example.legal.document.template.DocumentTemplateVersionRepository;
import com.example.legal.legalcase.CaseEntity;
import com.example.legal.legalcase.CaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesProvider;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGenerationService.class);
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final CaseRepository caseRepository;
    private final DocumentTemplateVersionRepository versionRepository;
    private final DocumentTemplateFieldRepository fieldRepository;
    private final DocumentGenerationRepository generationRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final GenerationValueResolver resolver;
    private final GenerationValueValidator valueValidator;
    private final TemplateDocumentRenderer renderer;
    private final DocumentStorage storage;
    private final DocumentGenerationPersistenceService persistenceService;

    public DocumentGenerationService(
            CaseRepository caseRepository,
            DocumentTemplateVersionRepository versionRepository,
            DocumentTemplateFieldRepository fieldRepository,
            DocumentGenerationRepository generationRepository,
            CaseDocumentRepository caseDocumentRepository,
            GenerationValueResolver resolver,
            GenerationValueValidator valueValidator,
            TemplateDocumentRenderer renderer,
            DocumentStorage storage,
            DocumentGenerationPersistenceService persistenceService
    ) {
        this.caseRepository = caseRepository;
        this.versionRepository = versionRepository;
        this.fieldRepository = fieldRepository;
        this.generationRepository = generationRepository;
        this.caseDocumentRepository = caseDocumentRepository;
        this.resolver = resolver;
        this.valueValidator = valueValidator;
        this.renderer = renderer;
        this.storage = storage;
        this.persistenceService = persistenceService;
    }

    public GenerationPreparation prepare(Long caseId, Long templateId, int versionNumber, String timezone) {
        CaseEntity legalCase = requireCase(caseId);
        DocumentTemplateVersionEntity version = requireVersion(templateId, versionNumber);
        ZoneId zone = requireTimezone(timezone);
        List<PreparedGenerationField> fields = fields(version).stream()
                .map(field -> preparedField(field, legalCase, zone))
                .toList();
        return new GenerationPreparation(caseId, templateId, versionNumber, zone.getId(), fields);
    }

    public GeneratedDocument generate(DocumentGenerationCommand command) {
        validateCommand(command);
        CaseEntity legalCase = requireCase(command.caseId());
        DocumentTemplateVersionEntity version = requireVersion(command.templateId(), command.versionNumber());
        ZoneId zone = requireTimezone(command.timezone());
        String idempotencyKey = requireIdempotencyKey(command.idempotencyKey());
        List<GenerationValueValidator.ValidatedGenerationValue> values = valueValidator.validateRequest(
                fields(version), command.values()
        );
        String fingerprint = fingerprint(command, version, zone, values);

        DocumentGenerationEntity existing = generationRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return replay(existing, fingerprint, version);
        }

        valueValidator.validateDeterministicSources(values, legalCase, zone);

        byte[] template = verifiedTemplate(version);
        Map<String, String> renderValues = new LinkedHashMap<>();
        values.forEach(value -> renderValues.put(value.field().getFieldKey(), value.value()));
        byte[] output = render(template, renderValues);
        StoredDocument stored = store(output);
        if (stored.contentLength() != output.length) {
            DocumentGenerationException failure = storageFailure("Stored output length does not match rendered content", null);
            compensate(stored.storageKey(), failure);
            throw failure;
        }

        try {
            PersistedDocumentGeneration persisted = persistenceService.persist(new GenerationPersistenceCommand(
                    command.caseId(), version.getId(), legalCase.getStatus(), idempotencyKey, fingerprint,
                    outputFileName(command.templateId(), command.versionNumber()), stored.storageKey(),
                    DOCX_CONTENT_TYPE, stored.contentLength(), values.stream()
                    .map(value -> new GenerationValueToPersist(
                            value.field().getId(), value.value(), value.valueSource()
                    ))
                    .toList()
            ));
            return response(persisted.generation(), persisted.caseDocument(), version);
        } catch (RuntimeException persistenceFailure) {
            compensate(stored.storageKey(), persistenceFailure);
            DocumentGenerationEntity winner = generationRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (winner != null) {
                return replay(winner, fingerprint, version);
            }
            if (persistenceFailure instanceof DocumentGenerationException generationFailure) {
                throw generationFailure;
            }
            throw new DocumentGenerationException(
                    GenerationErrorCode.GENERATION_PERSISTENCE_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Generated document metadata could not be persisted",
                    persistenceFailure
            );
        }
    }

    private PreparedGenerationField preparedField(
            DocumentTemplateFieldEntity field,
            CaseEntity legalCase,
            ZoneId zone
    ) {
        String suggestion = resolver.resolve(field, legalCase, zone);
        if (!valueValidator.isUsableSuggestion(field, suggestion)) {
            suggestion = null;
        }
        return new PreparedGenerationField(
                field.getFieldKey(), field.getDisplayName(), field.getDescription(), field.getValueType(),
                field.isRequired(), field.getDefaultSource(), field.getSourceKey(), field.getDisplayOrder(),
                suggestion, suggestion == null ? GenerationValueStatus.REQUIRES_USER_INPUT : GenerationValueStatus.RESOLVED
        );
    }

    private byte[] verifiedTemplate(DocumentTemplateVersionEntity version) {
        if (version.getFileSize() < 0 || version.getFileSize() > Integer.MAX_VALUE - 1) {
            throw integrityFailure("Published template size is invalid", null);
        }
        try (InputStream input = storage.open(version.getStorageKey())) {
            byte[] content = input.readNBytes((int) version.getFileSize() + 1);
            if (content.length != version.getFileSize()
                    || !sha256(content).equals(version.getContentSha256())) {
                throw integrityFailure("Published template content does not match its immutable metadata", null);
            }
            return content;
        } catch (DocumentGenerationException exception) {
            throw exception;
        } catch (DocumentStorageException | IOException exception) {
            throw integrityFailure("Published template content could not be verified", exception);
        }
    }

    private byte[] render(byte[] template, Map<String, String> values) {
        try {
            return renderer.render(template, values);
        } catch (TemplateRenderingException exception) {
            throw new DocumentGenerationException(
                    GenerationErrorCode.GENERATION_RENDERING_FAILED,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Published template could not be rendered with the reviewed values",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new DocumentGenerationException(
                    GenerationErrorCode.GENERATION_RENDERING_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Document rendering failed",
                    exception
            );
        }
    }

    private StoredDocument store(byte[] output) {
        try {
            return storage.store(new DocumentStorageRequest(new ByteArrayInputStream(output), output.length));
        } catch (DocumentStorageException exception) {
            throw storageFailure("Generated document could not be stored", exception);
        }
    }

    private void compensate(String storageKey, RuntimeException original) {
        try {
            storage.remove(storageKey);
        } catch (RuntimeException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
            LOGGER.warn("Generated document compensation failed for storage key {}", storageKey, cleanupFailure);
        }
    }

    private GeneratedDocument replay(
            DocumentGenerationEntity existing,
            String fingerprint,
            DocumentTemplateVersionEntity version
    ) {
        if (!existing.getRequestSha256().equals(fingerprint)) {
            throw new DocumentGenerationException(
                    GenerationErrorCode.GENERATION_IDEMPOTENCY_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Idempotency key was already used for a different generation request"
            );
        }
        CaseDocumentEntity output = existing.getCaseDocumentId() == null
                ? null
                : caseDocumentRepository.findById(existing.getCaseDocumentId()).orElse(null);
        return response(existing, output, version);
    }

    private GeneratedDocument response(
            DocumentGenerationEntity generation,
            CaseDocumentEntity output,
            DocumentTemplateVersionEntity version
    ) {
        return new GeneratedDocument(
                generation.getId(), generation.getCaseId(), version.getTemplateId(), version.getVersionNumber(),
                output == null ? null : output.getId(), output != null,
                output == null ? null : output.getOriginalFileName(),
                generation.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }

    private List<DocumentTemplateFieldEntity> fields(DocumentTemplateVersionEntity version) {
        return fieldRepository.findAllByTemplateVersionIdOrderByDisplayOrder(version.getId());
    }

    private CaseEntity requireCase(Long caseId) {
        if (caseId == null || caseId <= 0) {
            throw notFound(GenerationErrorCode.GENERATION_CASE_NOT_FOUND, "Case was not found");
        }
        return caseRepository.findById(caseId)
                .orElseThrow(() -> notFound(GenerationErrorCode.GENERATION_CASE_NOT_FOUND, "Case was not found"));
    }

    private DocumentTemplateVersionEntity requireVersion(Long templateId, int versionNumber) {
        if (templateId == null || templateId <= 0 || versionNumber <= 0) {
            throw notFound(GenerationErrorCode.GENERATION_TEMPLATE_VERSION_NOT_FOUND, "Template version was not found");
        }
        return versionRepository.findByTemplateIdAndVersionNumber(templateId, versionNumber)
                .orElseThrow(() -> notFound(
                        GenerationErrorCode.GENERATION_TEMPLATE_VERSION_NOT_FOUND,
                        "Template version was not found"
                ));
    }

    private ZoneId requireTimezone(String timezone) {
        if (timezone == null || !ZoneRulesProvider.getAvailableZoneIds().contains(timezone)) {
            throw invalid("timezone must be a recognized IANA timezone identifier");
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw invalid("timezone must be a recognized IANA timezone identifier");
        }
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        try {
            String canonical = UUID.fromString(idempotencyKey).toString();
            if (!canonical.equals(idempotencyKey)) {
                throw invalid("Idempotency-Key must be a canonical lowercase UUID");
            }
            return canonical;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalid("Idempotency-Key must be a canonical lowercase UUID");
        }
    }

    private void validateCommand(DocumentGenerationCommand command) {
        if (command == null) {
            throw invalid("Generation request is required");
        }
    }

    private String fingerprint(
            DocumentGenerationCommand command,
            DocumentTemplateVersionEntity version,
            ZoneId zone,
            List<GenerationValueValidator.ValidatedGenerationValue> values
    ) {
        MessageDigest digest = digest();
        update(digest, String.valueOf(command.caseId()));
        update(digest, String.valueOf(command.templateId()));
        update(digest, String.valueOf(version.getId()));
        update(digest, String.valueOf(version.getVersionNumber()));
        update(digest, zone.getId());
        values.stream()
                .sorted(Comparator.comparing(value -> value.field().getFieldKey()))
                .forEach(value -> {
                    update(digest, value.field().getFieldKey());
                    update(digest, value.value());
                    update(digest, value.valueSource().name());
                });
        return HexFormat.of().formatHex(digest.digest());
    }

    private String sha256(byte[] content) {
        return HexFormat.of().formatHex(digest().digest(content));
    }

    private MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void update(MessageDigest digest, String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(encoded.length).array());
        digest.update(encoded);
    }

    private String outputFileName(Long templateId, int versionNumber) {
        return "generated-template-" + templateId + "-v" + versionNumber + ".docx";
    }

    private DocumentGenerationException invalid(String message) {
        return new DocumentGenerationException(
                GenerationErrorCode.GENERATION_REQUEST_INVALID,
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private DocumentGenerationException notFound(GenerationErrorCode code, String message) {
        return new DocumentGenerationException(code, HttpStatus.NOT_FOUND, message);
    }

    private DocumentGenerationException integrityFailure(String message, Throwable cause) {
        return new DocumentGenerationException(
                GenerationErrorCode.GENERATION_TEMPLATE_INTEGRITY_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                cause
        );
    }

    private DocumentGenerationException storageFailure(String message, Throwable cause) {
        return new DocumentGenerationException(
                GenerationErrorCode.GENERATION_STORAGE_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                cause
        );
    }
}
