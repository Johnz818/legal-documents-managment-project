package com.example.legal.document.generation;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.template.DocumentTemplateFieldRepository;
import com.example.legal.legalcase.CaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentGenerationPersistenceServiceTest {

    @Test
    void capturesOneFixedUtcTimestampForTheImmutableGeneration() {
        CaseDocumentRepository documentRepository = mock(CaseDocumentRepository.class);
        DocumentGenerationRepository generationRepository = mock(DocumentGenerationRepository.class);
        GenerationValueRepository valueRepository = mock(GenerationValueRepository.class);
        DocumentTemplateFieldRepository fieldRepository = mock(DocumentTemplateFieldRepository.class);
        Instant instant = Instant.parse("2026-08-06T16:20:17.123456Z");
        DocumentGenerationPersistenceService service = new DocumentGenerationPersistenceService(
                documentRepository,
                generationRepository,
                valueRepository,
                fieldRepository,
                Clock.fixed(instant, ZoneOffset.UTC)
        );
        when(fieldRepository.findAllByTemplateVersionIdOrderByDisplayOrder(22L)).thenReturn(List.of());
        when(documentRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            CaseDocumentEntity document = invocation.getArgument(0);
            ReflectionTestUtils.setField(document, "id", 44L);
            return document;
        });
        when(generationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PersistedDocumentGeneration persisted = service.persist(new GenerationPersistenceCommand(
                7L, 22L, CaseStatus.IN_TRIAL, UUID.randomUUID().toString(), "a".repeat(64),
                "generated.docx", "storage-key",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                10, List.of()
        ));

        assertThat(persisted.generation().getCreatedAt())
                .isEqualTo(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
