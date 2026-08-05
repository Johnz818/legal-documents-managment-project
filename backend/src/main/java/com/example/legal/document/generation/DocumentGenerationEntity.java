package com.example.legal.document.generation;

import com.example.legal.legalcase.CaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(
        name = "document_generations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_generations_case_document",
                        columnNames = "case_document_id"
                ),
                @UniqueConstraint(
                        name = "uk_document_generations_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
public class DocumentGenerationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "template_version_id", nullable = false)
    private Long templateVersionId;

    @Column(name = "case_document_id")
    private Long caseDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_status_snapshot", nullable = false, length = 50)
    private CaseStatus caseStatusSnapshot;

    @Column(name = "idempotency_key", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String idempotencyKey;

    @Column(name = "request_sha256", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String requestSha256;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DocumentGenerationEntity() {
    }

    public DocumentGenerationEntity(
            Long caseId,
            Long templateVersionId,
            Long caseDocumentId,
            CaseStatus caseStatusSnapshot,
            String idempotencyKey,
            String requestSha256
    ) {
        this.caseId = caseId;
        this.templateVersionId = templateVersionId;
        this.caseDocumentId = caseDocumentId;
        this.caseStatusSnapshot = caseStatusSnapshot;
        this.idempotencyKey = idempotencyKey;
        this.requestSha256 = requestSha256;
    }

    public Long getId() {
        return id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public Long getTemplateVersionId() {
        return templateVersionId;
    }

    public Long getCaseDocumentId() {
        return caseDocumentId;
    }

    public CaseStatus getCaseStatusSnapshot() {
        return caseStatusSnapshot;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestSha256() {
        return requestSha256;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
