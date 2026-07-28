package com.example.legal.document.read;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/cases/{caseId}/documents")
public class CaseDocumentReadController {

    private final CaseDocumentQueryService documentQueryService;

    public CaseDocumentReadController(CaseDocumentQueryService documentQueryService) {
        this.documentQueryService = documentQueryService;
    }

    @GetMapping
    public CaseDocumentListResponse getCaseDocuments(@PathVariable Long caseId) {
        return documentQueryService.getCaseDocuments(caseId);
    }

    @GetMapping("/{documentId}/content")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long caseId,
            @PathVariable Long documentId
    ) {
        DocumentDownload download = documentQueryService.download(caseId, documentId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(download.content()));
    }

    @ExceptionHandler(DocumentReadNotFoundException.class)
    ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }
}
