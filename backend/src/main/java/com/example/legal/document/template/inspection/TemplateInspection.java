package com.example.legal.document.template.inspection;

import java.util.List;

public record TemplateInspection(List<DetectedTemplateMarker> markers) {

    public TemplateInspection {
        markers = List.copyOf(markers);
    }
}
