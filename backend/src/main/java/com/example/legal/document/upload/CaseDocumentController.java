package com.example.legal.document.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

    @ExceptionHandler(InvalidDocumentUploadException.class)
    ResponseEntity<Void> handleInvalidUpload() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(DocumentCaseNotFoundException.class)
    ResponseEntity<Void> handleMissingCase() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({DocumentTooLargeException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<Void> handleOversizedUpload() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }

    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    ResponseEntity<Void> handleUnsupportedType() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }
}
