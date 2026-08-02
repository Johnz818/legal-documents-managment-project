package com.example.legal.document.template.api;

import com.example.legal.document.template.inspection.DetectedTemplateMarker;
import com.example.legal.document.template.inspection.TemplateInspection;

import java.util.List;

public record TemplateInspectionResponse(List<DetectedTemplateMarker> markers) {

    static TemplateInspectionResponse from(TemplateInspection inspection) {
        return new TemplateInspectionResponse(inspection.markers());
    }
}
