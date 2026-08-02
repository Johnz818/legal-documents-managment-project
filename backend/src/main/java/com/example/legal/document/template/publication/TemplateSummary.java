package com.example.legal.document.template.publication;

import com.example.legal.document.template.DocumentTemplateType;

import java.time.LocalDateTime;

public record TemplateSummary(
        Long id,
        String name,
        String description,
        DocumentTemplateType templateType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
