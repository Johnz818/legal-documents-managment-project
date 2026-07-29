package com.example.legal.document.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cases/{caseId}/documents")
public class CaseDocumentController {

    private final DocumentUploadService documentUploadService;

    public CaseDocumentController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CaseDocumentResponse> upload(
            @PathVariable Long caseId,
            @RequestPart("file") MultipartFile file
    ) {
        CaseDocumentResponse response = documentUploadService.upload(
                caseId,
                new DocumentUploadCommand(
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getSize(),
                        file::getInputStream
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
