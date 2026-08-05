package com.example.legal.document.generation.api;

import com.example.legal.document.generation.DocumentGenerationCommand;
import com.example.legal.document.generation.DocumentGenerationException;
import com.example.legal.document.generation.DocumentGenerationService;
import com.example.legal.document.generation.GeneratedDocument;
import com.example.legal.document.generation.GenerationErrorCode;
import com.example.legal.document.generation.GenerationPreparation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cases/{caseId}/document-generations")
public class DocumentGenerationController {

    private final DocumentGenerationService service;

    public DocumentGenerationController(DocumentGenerationService service) {
        this.service = service;
    }

    @GetMapping("/preparation")
    public GenerationPreparation prepare(
            @PathVariable Long caseId,
            @RequestParam Long templateId,
            @RequestParam int versionNumber,
            @RequestParam String timezone
    ) {
        return service.prepare(caseId, templateId, versionNumber, timezone);
    }

    @PostMapping
    public ResponseEntity<GeneratedDocument> generate(
            @PathVariable Long caseId,
            @RequestParam Long templateId,
            @RequestParam int versionNumber,
            @RequestParam String timezone,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) DocumentGenerationRequest request
    ) {
        if (request == null || request.values() == null) {
            throw invalidRequest();
        }
        List<com.example.legal.document.generation.ReviewedGenerationValue> values;
        try {
            values = request.values().stream().map(GenerationValueRequest::toValue).toList();
        } catch (RuntimeException exception) {
            throw invalidRequest();
        }
        GeneratedDocument generated = service.generate(new DocumentGenerationCommand(
                caseId, templateId, versionNumber, timezone, idempotencyKey, values
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(generated);
    }

    private DocumentGenerationException invalidRequest() {
        return new DocumentGenerationException(
                GenerationErrorCode.GENERATION_REQUEST_INVALID,
                HttpStatus.BAD_REQUEST,
                "Generation request is invalid"
        );
    }
}
