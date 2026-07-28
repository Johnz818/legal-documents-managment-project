package com.example.legal.document.upload;

import com.example.legal.document.CaseDocumentEntity;
import com.example.legal.document.CaseDocumentRepository;
import com.example.legal.document.DocumentSource;
import com.example.legal.legalcase.CaseRepository;
import com.example.legal.document.storage.DocumentStorage;
import com.example.legal.document.storage.DocumentStorageRequest;
import com.example.legal.document.storage.StoredDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DocumentUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUploadService.class);

    private final CaseRepository caseRepository;
    private final CaseDocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentUploadValidator validator;

    public DocumentUploadService(
            CaseRepository caseRepository,
            CaseDocumentRepository documentRepository,
            DocumentStorage documentStorage,
            DocumentUploadValidator validator
    ) {
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.validator = validator;
    }

    @Transactional
    public CaseDocumentResponse upload(Long caseId, DocumentUploadCommand command) {
        if (!caseRepository.existsById(caseId)) {
            throw new DocumentCaseNotFoundException(caseId);
        }

        ValidatedDocumentUpload validated = validator.validate(command);
        StoredDocument storedDocument = store(command, validated);

        try {
            CaseDocumentEntity document = documentRepository.saveAndFlush(new CaseDocumentEntity(
                    caseId,
                    validated.originalFileName(),
                    storedDocument.storageKey(),
                    DocumentSource.UPLOADED,
                    validated.fileFormat(),
                    validated.contentType(),
                    storedDocument.contentLength()
            ));
            return toResponse(document);
        } catch (RuntimeException exception) {
            removeOrphanedContent(storedDocument.storageKey());
            throw exception;
        }
    }

    private StoredDocument store(
            DocumentUploadCommand command,
            ValidatedDocumentUpload validated
    ) {
        try (InputStream content = command.contentSource().openStream()) {
            return documentStorage.store(new DocumentStorageRequest(
                    content,
                    validated.contentLength()
            ));
        } catch (IOException exception) {
            throw new DocumentUploadProcessingException("Unable to read document content", exception);
        }
    }

    private void removeOrphanedContent(String storageKey) {
        try {
            documentStorage.remove(storageKey);
        } catch (RuntimeException cleanupFailure) {
            LOGGER.error("Unable to remove document content after metadata persistence failure", cleanupFailure);
        }
    }

    private CaseDocumentResponse toResponse(CaseDocumentEntity document) {
        return new CaseDocumentResponse(
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
