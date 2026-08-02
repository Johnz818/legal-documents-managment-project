package com.example.legal.document.template.publication;

import java.time.LocalDateTime;
import java.util.List;

public record PublishedTemplateVersion(
        Long templateId,
        String templateName,
        String templateDescription,
        int versionNumber,
        String originalFileName,
        String contentType,
        long fileSize,
        String contentSha256,
        LocalDateTime publishedAt,
        List<PublishedTemplateField> fields
) {
    public PublishedTemplateVersion {
        fields = List.copyOf(fields);
    }
}
