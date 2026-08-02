package com.example.legal.document.template.publication;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;

public record PublishedTemplateField(
        String fieldKey,
        String displayName,
        String description,
        DocumentFieldValueType valueType,
        boolean required,
        DocumentFieldDefaultSource defaultSource,
        String sourceKey,
        int displayOrder
) {
}
