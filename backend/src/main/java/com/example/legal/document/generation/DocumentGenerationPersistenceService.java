package com.example.legal.document.generation;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentFormat;
import com.example.legal.document.DocumentSource;
import com.example.legal.document.template.DocumentTemplateFieldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DocumentGenerationPersistenceService {

    private final CaseDocumentRepository caseDocumentRepository;
    private final DocumentGenerationRepository generationRepository;
    private final GenerationValueRepository valueRepository;
    private final DocumentTemplateFieldRepository templateFieldRepository;

    public DocumentGenerationPersistenceService(
            CaseDocumentRepository caseDocumentRepository,
            DocumentGenerationRepository generationRepository,
            GenerationValueRepository valueRepository,
            DocumentTemplateFieldRepository templateFieldRepository
    ) {
        this.caseDocumentRepository = caseDocumentRepository;
        this.generationRepository = generationRepository;
        this.valueRepository = valueRepository;
        this.templateFieldRepository = templateFieldRepository;
    }

    @Transactional
    public PersistedDocumentGeneration persist(GenerationPersistenceCommand command) {
        validateTemplateFields(command);
        CaseDocumentEntity caseDocument = caseDocumentRepository.saveAndFlush(new CaseDocumentEntity(
                command.caseId(),
                command.fileName(),
                command.storageKey(),
                DocumentSource.GENERATED,
                DocumentFormat.DOCX,
                command.contentType(),
                command.fileSize()
        ));

        DocumentGenerationEntity generation = generationRepository.saveAndFlush(
                new DocumentGenerationEntity(
                        command.caseId(),
                        command.templateVersionId(),
                        caseDocument.getId(),
                        command.caseStatusSnapshot(),
                        command.idempotencyKey(),
                        command.requestSha256()
                )
        );

        List<GenerationValueEntity> values = new ArrayList<>(command.values().size());
        for (GenerationValueToPersist value : command.values()) {
            values.add(valueRepository.save(new GenerationValueEntity(
                    generation.getId(),
                    value.templateFieldId(),
                    value.resolvedValue(),
                    value.valueSource()
            )));
        }
        valueRepository.flush();

        return new PersistedDocumentGeneration(generation, caseDocument, values);
    }

    private void validateTemplateFields(GenerationPersistenceCommand command) {
        Set<Long> versionFieldIds = new HashSet<>(templateFieldRepository
                .findAllByTemplateVersionIdOrderByDisplayOrder(command.templateVersionId())
                .stream()
                .map(field -> field.getId())
                .toList());
        Set<Long> submittedFieldIds = new HashSet<>();
        for (GenerationValueToPersist value : command.values()) {
            if (!versionFieldIds.contains(value.templateFieldId())) {
                throw new IllegalArgumentException(
                        "Generation value must reference a field owned by the selected template version"
                );
            }
            if (!submittedFieldIds.add(value.templateFieldId())) {
                throw new IllegalArgumentException(
                        "Generation values must not repeat a template field"
                );
            }
        }
        if (!submittedFieldIds.equals(versionFieldIds)) {
            throw new IllegalArgumentException(
                    "Generation values must cover the selected template version field contract"
            );
        }
    }
}
