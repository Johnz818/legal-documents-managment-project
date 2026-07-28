package com.example.legal.document.read;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.legalcase.CaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CaseDocumentQueryService {

    private final CaseRepository caseRepository;
    private final CaseDocumentRepository documentRepository;
    private final DocumentStorage documentStorage;

    public CaseDocumentQueryService(
            CaseRepository caseRepository,
            CaseDocumentRepository documentRepository,
            DocumentStorage documentStorage
    ) {
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
    }

    @Transactional(readOnly = true)
    public CaseDocumentListResponse getCaseDocuments(Long caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new DocumentReadNotFoundException();
        }

        List<CaseDocumentSummaryResponse> documents = documentRepository
                .findAllByCaseIdOrderByCreatedAtDescIdDesc(caseId)
                .stream()
                .map(this::toSummary)
                .toList();
        return new CaseDocumentListResponse(documents);
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

    private CaseDocumentSummaryResponse toSummary(CaseDocumentEntity document) {
        return new CaseDocumentSummaryResponse(
                document.getId(),
                document.getCaseId(),
                document.getOriginalFileName(),
                document.getDocumentSource(),
                document.getFileFormat(),
                document.getContentType(),
                document.getFileSize(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
