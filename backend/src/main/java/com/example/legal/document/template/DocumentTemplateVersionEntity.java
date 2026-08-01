package com.example.legal.document.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "document_template_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_template_versions_number",
                        columnNames = {"template_id", "version_number"}
                ),
                @UniqueConstraint(
                        name = "uk_document_template_versions_storage_key",
                        columnNames = "storage_key"
                )
        }
)
public class DocumentTemplateVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @CreationTimestamp
    @Column(name = "published_at", nullable = false, updatable = false)
    private LocalDateTime publishedAt;

    protected DocumentTemplateVersionEntity() {
    }

    public DocumentTemplateVersionEntity(
            Long templateId,
            int versionNumber,
            String originalFileName,
            String storageKey,
            String contentType,
            long fileSize,
            String contentSha256
    ) {
        this.templateId = templateId;
        this.versionNumber = versionNumber;
        this.originalFileName = originalFileName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.contentSha256 = contentSha256;
    }

    public Long getId() {
        return id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
