package com.example.legal.document.template.publication;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.inspection.TemplateMarker;

import java.util.List;

public record TemplateFieldDefinition(
        String fieldKey,
        String displayName,
        String description,
        DocumentFieldValueType valueType,
        boolean required,
        DocumentFieldDefaultSource defaultSource,
        String sourceKey,
        List<TemplateMarker> markers
) {
    public TemplateFieldDefinition {
        markers = markers == null ? List.of() : List.copyOf(markers);
    }
}
