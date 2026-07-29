package com.example.legal.document.removal;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.storage.DocumentStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentRemovalService {

    private final CaseDocumentRepository documentRepository;
    private final DocumentStorage documentStorage;

    public DocumentRemovalService(
            CaseDocumentRepository documentRepository,
            DocumentStorage documentStorage
    ) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
    }

    @Transactional
    public void remove(Long caseId, Long documentId) {
        CaseDocumentEntity document = documentRepository
                .findByIdAndCaseId(documentId, caseId)
                .orElseThrow(DocumentRemovalNotFoundException::new);

        documentRepository.delete(document);
        documentRepository.flush();
        documentStorage.remove(document.getStorageKey());
    }
}
