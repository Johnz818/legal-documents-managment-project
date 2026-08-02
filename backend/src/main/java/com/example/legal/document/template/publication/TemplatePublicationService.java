package com.example.legal.document.template.publication;

import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageException;
import com.example.legal.document.storage.DocumentStorageRequest;
import com.example.legal.document.storage.StoredDocument;
import com.example.legal.document.template.DocumentTemplateEntity;
import com.example.legal.document.template.DocumentTemplateFieldEntity;
import com.example.legal.document.template.DocumentTemplateFieldRepository;
import com.example.legal.document.template.DocumentTemplateRepository;
import com.example.legal.document.template.DocumentTemplateType;
import com.example.legal.document.template.DocumentTemplateVersionEntity;
import com.example.legal.document.template.DocumentTemplateVersionRepository;
import com.example.legal.document.template.inspection.TemplateInspection;
import com.example.legal.document.template.inspection.TemplateInspectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class TemplatePublicationService {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final TemplateInspectionService inspectionService;
    private final TemplateDocumentNormalizer normalizer;
    private final TemplatePublicationValidator validator;
    private final DocumentStorage storage;
    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateVersionRepository versionRepository;
    private final DocumentTemplateFieldRepository fieldRepository;
    private final TransactionTemplate transactionTemplate;

    public TemplatePublicationService(
            TemplateInspectionService inspectionService,
            TemplateDocumentNormalizer normalizer,
            TemplatePublicationValidator validator,
            DocumentStorage storage,
            DocumentTemplateRepository templateRepository,
            DocumentTemplateVersionRepository versionRepository,
            DocumentTemplateFieldRepository fieldRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.inspectionService = inspectionService;
        this.normalizer = normalizer;
        this.validator = validator;
        this.storage = storage;
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.fieldRepository = fieldRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PublishedTemplateVersion create(TemplatePublicationCommand command) {
        validateTemplateMetadata(command.name(), command.description());
        PreparedPublication prepared = prepare(command);
        StoredDocument stored = store(prepared.content());
        try {
            return transactionTemplate.execute(status -> {
                DocumentTemplateEntity template = templateRepository.saveAndFlush(
                        new DocumentTemplateEntity(
                                command.name().trim(),
                                trimToNull(command.description()),
                                DocumentTemplateType.CUSTOM
                        )
                );
                return persist(template, 1, command, prepared, stored);
            });
        } catch (RuntimeException exception) {
            compensate(stored.storageKey(), exception);
            throw persistenceFailure(exception);
        }
    }

    public PublishedTemplateVersion publishVersion(Long templateId, TemplatePublicationCommand command) {
        requireTemplate(templateId);
        PreparedPublication prepared = prepare(command);
        StoredDocument stored = store(prepared.content());
        try {
            return transactionTemplate.execute(status -> {
                DocumentTemplateEntity template = templateRepository.findByIdForUpdate(templateId)
                        .orElseThrow(() -> notFound(TemplatePublicationErrorCode.TEMPLATE_NOT_FOUND));
                int nextVersion = versionRepository
                        .findTopByTemplateIdOrderByVersionNumberDesc(templateId)
                        .map(version -> version.getVersionNumber() + 1)
                        .orElse(1);
                return persist(template, nextVersion, command, prepared, stored);
            });
        } catch (RuntimeException exception) {
            compensate(stored.storageKey(), exception);
            if (exception instanceof TemplatePublicationException publicationException) {
                throw publicationException;
            }
            throw persistenceFailure(exception);
        }
    }

    public TemplatePage<TemplateSummary> listTemplates(int page, int size) {
        PageRequest request = pageRequest(page, size);
        Page<TemplateSummary> result = templateRepository.findAllByOrderByCreatedAtDesc(request)
                .map(this::toSummary);
        return TemplatePage.from(result);
    }

    public TemplatePage<TemplateVersionSummary> listVersions(Long templateId, int page, int size) {
        requireTemplate(templateId);
        Page<TemplateVersionSummary> result = versionRepository
                .findAllByTemplateIdOrderByVersionNumberDesc(templateId, pageRequest(page, size))
                .map(this::toVersionSummary);
        return TemplatePage.from(result);
    }

    public PublishedTemplateVersion getVersion(Long templateId, int versionNumber) {
        DocumentTemplateEntity template = requireTemplate(templateId);
        DocumentTemplateVersionEntity version = versionRepository
                .findByTemplateIdAndVersionNumber(templateId, versionNumber)
                .orElseThrow(() -> notFound(TemplatePublicationErrorCode.TEMPLATE_VERSION_NOT_FOUND));
        return toPublished(template, version);
    }

    public TemplateDownload download(Long templateId, int versionNumber) {
        DocumentTemplateVersionEntity version = versionRepository
                .findByTemplateIdAndVersionNumber(templateId, versionNumber)
                .orElseThrow(() -> notFound(TemplatePublicationErrorCode.TEMPLATE_VERSION_NOT_FOUND));
        try {
            return new TemplateDownload(
                    version.getOriginalFileName(),
                    version.getContentType(),
                    version.getFileSize(),
                    storage.open(version.getStorageKey())
            );
        } catch (DocumentStorageException exception) {
            throw new TemplatePublicationException(
                    TemplatePublicationErrorCode.TEMPLATE_STORAGE_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Published template content could not be opened",
                    exception
            );
        }
    }

    private PreparedPublication prepare(TemplatePublicationCommand command) {
        if (command == null || command.content() == null) {
            throw invalid("Publication request is invalid");
        }
        byte[] source = command.content();
        TemplateInspection original = inspectionService.inspect(command.originalFileName(), source);
        Map<com.example.legal.document.template.inspection.TemplateMarker, String> mappings =
                validator.validate(original, command.fields());
        byte[] canonical = normalizer.normalize(source, mappings);
        TemplateInspection canonicalInspection = inspectionService.inspect(command.originalFileName(), canonical);
        validator.validateCanonicalResult(canonicalInspection, command.fields());
        return new PreparedPublication(canonical, sha256(canonical));
    }

    private StoredDocument store(byte[] content) {
        try {
            return storage.store(new DocumentStorageRequest(
                    new ByteArrayInputStream(content),
                    content.length
            ));
        } catch (DocumentStorageException exception) {
            throw new TemplatePublicationException(
                    TemplatePublicationErrorCode.TEMPLATE_STORAGE_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Template content could not be stored",
                    exception
            );
        }
    }

    private PublishedTemplateVersion persist(
            DocumentTemplateEntity template,
            int versionNumber,
            TemplatePublicationCommand command,
            PreparedPublication prepared,
            StoredDocument stored
    ) {
        DocumentTemplateVersionEntity version = versionRepository.saveAndFlush(
                new DocumentTemplateVersionEntity(
                        template.getId(),
                        versionNumber,
                        command.originalFileName(),
                        stored.storageKey(),
                        DOCX_CONTENT_TYPE,
                        stored.contentLength(),
                        prepared.sha256()
                )
        );
        for (int index = 0; index < command.fields().size(); index++) {
            TemplateFieldDefinition field = command.fields().get(index);
            fieldRepository.save(new DocumentTemplateFieldEntity(
                    version.getId(),
                    field.fieldKey(),
                    field.displayName().trim(),
                    trimToNull(field.description()),
                    field.valueType(),
                    field.required(),
                    field.defaultSource(),
                    field.sourceKey(),
                    index
            ));
        }
        fieldRepository.flush();
        return toPublished(template, version);
    }

    private PublishedTemplateVersion toPublished(
            DocumentTemplateEntity template,
            DocumentTemplateVersionEntity version
    ) {
        List<PublishedTemplateField> fields = fieldRepository
                .findAllByTemplateVersionIdOrderByDisplayOrder(version.getId())
                .stream()
                .map(field -> new PublishedTemplateField(
                        field.getFieldKey(), field.getDisplayName(), field.getDescription(),
                        field.getValueType(), field.isRequired(), field.getDefaultSource(),
                        field.getSourceKey(), field.getDisplayOrder()
                ))
                .toList();
        return new PublishedTemplateVersion(
                template.getId(), template.getName(), template.getDescription(),
                version.getVersionNumber(), version.getOriginalFileName(),
                version.getContentType(), version.getFileSize(), version.getContentSha256(),
                version.getPublishedAt(), fields
        );
    }

    private DocumentTemplateEntity requireTemplate(Long templateId) {
        if (templateId == null || templateId <= 0) {
            throw notFound(TemplatePublicationErrorCode.TEMPLATE_NOT_FOUND);
        }
        return templateRepository.findById(templateId)
                .orElseThrow(() -> notFound(TemplatePublicationErrorCode.TEMPLATE_NOT_FOUND));
    }

    private void validateTemplateMetadata(String name, String description) {
        if (name == null || name.isBlank() || name.length() > 200
                || description != null && description.length() > 1000) {
            throw invalid("Template name or description is invalid");
        }
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw invalid("Pagination must use page >= 0 and size between 1 and 100");
        }
        return PageRequest.of(page, size);
    }

    private TemplateSummary toSummary(DocumentTemplateEntity template) {
        return new TemplateSummary(
                template.getId(), template.getName(), template.getDescription(),
                template.getTemplateType(), template.getCreatedAt(), template.getUpdatedAt()
        );
    }

    private TemplateVersionSummary toVersionSummary(DocumentTemplateVersionEntity version) {
        return new TemplateVersionSummary(
                version.getVersionNumber(), version.getOriginalFileName(), version.getContentType(),
                version.getFileSize(), version.getContentSha256(), version.getPublishedAt()
        );
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void compensate(String storageKey, RuntimeException original) {
        try {
            storage.remove(storageKey);
        } catch (RuntimeException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
        }
    }

    private TemplatePublicationException persistenceFailure(RuntimeException cause) {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_PERSISTENCE_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Template publication metadata could not be persisted",
                cause
        );
    }

    private TemplatePublicationException invalid(String message) {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_PUBLICATION_INVALID,
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private TemplatePublicationException notFound(TemplatePublicationErrorCode code) {
        return new TemplatePublicationException(code, HttpStatus.NOT_FOUND, "Template resource was not found");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record PreparedPublication(byte[] content, String sha256) {
    }
}
