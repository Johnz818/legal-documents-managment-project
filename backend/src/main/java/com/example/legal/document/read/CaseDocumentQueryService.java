package com.example.legal.document.read;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentSource;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.generation.CaseDocumentGenerationTimestamp;
import com.example.legal.document.generation.DocumentGenerationRepository;
import com.example.legal.legalcase.CaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseDocumentQueryService {

    private final CaseRepository caseRepository;
    private final CaseDocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentGenerationRepository generationRepository;

    public CaseDocumentQueryService(
            CaseRepository caseRepository,
            CaseDocumentRepository documentRepository,
            DocumentStorage documentStorage,
            DocumentGenerationRepository generationRepository
    ) {
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.generationRepository = generationRepository;
    }

    @Transactional(readOnly = true)
    public CaseDocumentListResponse getCaseDocuments(Long caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new DocumentReadNotFoundException();
        }

        List<CaseDocumentEntity> documents = documentRepository
                .findAllByCaseIdOrderByCreatedAtDescIdDesc(caseId);
        List<Long> generatedDocumentIds = documents.stream()
                .filter(document -> document.getDocumentSource() == DocumentSource.GENERATED)
                .map(CaseDocumentEntity::getId)
                .toList();
        Map<Long, Instant> generatedAtByDocumentId = generatedDocumentIds.isEmpty()
                ? Map.of()
                : generationRepository.findCaseDocumentTimestamps(caseId, generatedDocumentIds).stream()
                .collect(Collectors.toUnmodifiableMap(
                        CaseDocumentGenerationTimestamp::getCaseDocumentId,
                        timestamp -> timestamp.getCreatedAt().toInstant(ZoneOffset.UTC)
                ));

        List<CaseDocumentSummaryResponse> summaries = documents.stream()
                .map(document -> toSummary(
                        document,
                        document.getDocumentSource() == DocumentSource.GENERATED
                                ? generatedAtByDocumentId.get(document.getId())
                                : null
                ))
                .toList();
        return new CaseDocumentListResponse(summaries);
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(Long caseId, Long documentId) {
        CaseDocumentEntity document = documentRepository
                .findByIdAndCaseId(documentId, caseId)
                .orElseThrow(DocumentReadNotFoundException::new);

        return new DocumentDownload(
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                documentStorage.open(document.getStorageKey())
        );
    }

    private CaseDocumentSummaryResponse toSummary(CaseDocumentEntity document, Instant generatedAt) {
        return new CaseDocumentSummaryResponse(
                document.getId(),
                document.getCaseId(),
                document.getOriginalFileName(),
                document.getDocumentSource(),
                document.getFileFormat(),
                document.getContentType(),
                document.getFileSize(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                generatedAt
        );
    }
}
