package com.example.legal.legalcase;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseQueryService caseQueryService;
    private final CaseCommandService caseCommandService;

    public CaseController(
            CaseQueryService caseQueryService,
            CaseCommandService caseCommandService
    ) {
        this.caseQueryService = caseQueryService;
        this.caseCommandService = caseCommandService;
    }

    @GetMapping
    public CaseListResponse getCases(
            @RequestParam(required = false) String caseNumberPrefix,
            @RequestParam(required = false) String caseNamePrefix,
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) String leadLawyerName
    ) {
        return caseQueryService.getCases(
                caseNumberPrefix,
                caseNamePrefix,
                status,
                leadLawyerName
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseDetailResponse> getCaseById(@PathVariable Long id) {
        return caseQueryService.getCaseById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CaseDetailResponse> createCase(
            @Valid @RequestBody CaseCreateRequest request
    ) {
        CaseDetailResponse response = caseCommandService.createCase(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public CaseDetailResponse updateCase(
            @PathVariable Long id,
            @Valid @RequestBody CaseUpdateRequest request
    ) {
        return caseCommandService.updateCase(id, request);
    }

    @ExceptionHandler(DuplicateCaseNumberException.class)
    public ResponseEntity<Void> handleDuplicateCaseNumber() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Void> handleCaseNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(StaleCaseVersionException.class)
    public ResponseEntity<Void> handleStaleCaseVersion() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
