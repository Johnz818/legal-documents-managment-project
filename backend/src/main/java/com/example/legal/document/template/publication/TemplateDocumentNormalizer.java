package com.example.legal.document.template.publication;

import com.example.legal.document.template.inspection.TemplateMarker;

import java.util.Map;

public interface TemplateDocumentNormalizer {

    byte[] normalize(byte[] source, Map<TemplateMarker, String> markerMappings);
}
