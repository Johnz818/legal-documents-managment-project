package com.example.legal.document.template.inspection;

public record DetectedTemplateMarker(
        TemplateMarkerKind kind,
        String value,
        int occurrenceCount
) {
}
