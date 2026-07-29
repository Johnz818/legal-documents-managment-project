package com.example.legal.document.removal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases/{caseId}/documents")
public class DocumentRemovalController {

    private final DocumentRemovalService documentRemovalService;

    public DocumentRemovalController(DocumentRemovalService documentRemovalService) {
        this.documentRemovalService = documentRemovalService;
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long caseId,
            @PathVariable Long documentId
    ) {
        documentRemovalService.remove(caseId, documentId);
        return ResponseEntity.noContent().build();
    }
}
