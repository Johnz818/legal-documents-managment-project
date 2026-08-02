package com.example.legal.document.template.inspection;

import java.util.Objects;

public record TemplateMarker(TemplateMarkerKind kind, String value) {

    public TemplateMarker {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(value, "value");
    }
}
