package com.example.legal.document.template.publication;

import java.time.LocalDateTime;

public record TemplateVersionSummary(
        int versionNumber,
        String originalFileName,
        String contentType,
        long fileSize,
        String contentSha256,
        LocalDateTime publishedAt
) {
}
