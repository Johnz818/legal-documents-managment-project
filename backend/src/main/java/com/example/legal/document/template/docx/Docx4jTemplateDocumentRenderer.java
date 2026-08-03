package com.example.legal.document.template.docx;

import com.example.legal.document.generation.TemplateDocumentRenderer;
import com.example.legal.document.generation.TemplateRenderingErrorCode;
import com.example.legal.document.generation.TemplateRenderingException;
import com.example.legal.document.template.inspection.DetectedTemplateMarker;
import com.example.legal.document.template.inspection.TemplateDocumentInspector;
import com.example.legal.document.template.inspection.TemplateInspection;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class Docx4jTemplateDocumentRenderer implements TemplateDocumentRenderer {

    private final TemplateDocumentInspector inspector;
    private final TemplateMarkerParser markerParser = new TemplateMarkerParser();
    private final Docx4jTextTokenReplacer tokenReplacer = new Docx4jTextTokenReplacer();

    public Docx4jTemplateDocumentRenderer(TemplateDocumentInspector inspector) {
        this.inspector = inspector;
    }

    @Override
    public byte[] render(byte[] templateContent, Map<String, String> values) {
        Map<String, String> safeValues = requireValues(values);
        TemplateInspection inspection = inspect(templateContent);
        Set<String> expectedKeys = canonicalKeys(inspection);
        requireExactKeys(expectedKeys, safeValues.keySet());
        validateScalarValues(safeValues);

        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.load(
                    new ByteArrayInputStream(templateContent)
            );
            tokenReplacer.replace(wordPackage, text -> replacements(text, safeValues));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            wordPackage.save(output);
            return output.toByteArray();
        } catch (TemplateRenderingException exception) {
            throw exception;
        } catch (Docx4JException | RuntimeException exception) {
            throw new TemplateRenderingException(
                    TemplateRenderingErrorCode.TEMPLATE_RENDERING_FAILED,
                    "Unable to render DOCX template",
                    exception
            );
        }
    }

    private Map<String, String> requireValues(Map<String, String> values) {
        if (values == null) {
            throw invalidInput("Rendering values must not be null", Map.of());
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw invalidInput("Rendering keys and values must not be null", Map.of());
            }
        }
        return Map.copyOf(values);
    }

    private TemplateInspection inspect(byte[] templateContent) {
        try {
            return inspector.inspect(templateContent);
        } catch (TemplateInspectionException exception) {
            throw new TemplateRenderingException(
                    TemplateRenderingErrorCode.TEMPLATE_RENDERING_TEMPLATE_UNSUPPORTED,
                    "Template cannot be rendered",
                    Map.of(
                            "inspectionCode", exception.getCode().name(),
                            "inspectionDetails", exception.getDetails()
                    )
            );
        }
    }

    private Set<String> canonicalKeys(TemplateInspection inspection) {
        Set<String> keys = new HashSet<>();
        List<String> nonCanonicalMarkers = new ArrayList<>();
        for (DetectedTemplateMarker marker : inspection.markers()) {
            if (marker.kind() == TemplateMarkerKind.CANONICAL) {
                keys.add(marker.value());
            } else {
                nonCanonicalMarkers.add(marker.value());
            }
        }
        if (!nonCanonicalMarkers.isEmpty()) {
            throw new TemplateRenderingException(
                    TemplateRenderingErrorCode.TEMPLATE_RENDERING_TEMPLATE_UNSUPPORTED,
                    "Template contains non-canonical markers",
                    Map.of("markers", List.copyOf(nonCanonicalMarkers))
            );
        }
        return Set.copyOf(keys);
    }

    private void requireExactKeys(Set<String> expectedKeys, Set<String> suppliedKeys) {
        TreeSet<String> missing = new TreeSet<>(expectedKeys);
        missing.removeAll(suppliedKeys);
        TreeSet<String> extra = new TreeSet<>(suppliedKeys);
        extra.removeAll(expectedKeys);
        if (!missing.isEmpty() || !extra.isEmpty()) {
            throw invalidInput(
                    "Rendering values must exactly match the template fields",
                    Map.of("missingKeys", List.copyOf(missing), "extraKeys", List.copyOf(extra))
            );
        }
    }

    private void validateScalarValues(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue();
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw invalidInput(
                        "Rendering values must be single-line scalar text",
                        Map.of("fieldKey", entry.getKey())
                );
            }
        }
    }

    private List<Docx4jTextTokenReplacer.TextReplacement> replacements(
            String paragraphText,
            Map<String, String> values
    ) {
        List<Docx4jTextTokenReplacer.TextReplacement> replacements = new ArrayList<>();
        int searchFrom = 0;
        for (var marker : markerParser.parse(paragraphText, "MAIN_BODY")) {
            if (marker.kind() != TemplateMarkerKind.CANONICAL) {
                throw new TemplateRenderingException(
                        TemplateRenderingErrorCode.TEMPLATE_RENDERING_TEMPLATE_UNSUPPORTED,
                        "Template contains non-canonical markers",
                        Map.of("marker", marker.value())
                );
            }
            String token = "{{" + marker.value() + "}}";
            int start = paragraphText.indexOf(token, searchFrom);
            String value = values.get(marker.value());
            if (start < 0 || value == null) {
                throw invalidInput(
                        "Rendering value does not match a template marker",
                        Map.of("fieldKey", marker.value())
                );
            }
            replacements.add(new Docx4jTextTokenReplacer.TextReplacement(
                    start, start + token.length(), value
            ));
            searchFrom = start + token.length();
        }
        return replacements;
    }

    private TemplateRenderingException invalidInput(String message, Map<String, Object> details) {
        return new TemplateRenderingException(
                TemplateRenderingErrorCode.TEMPLATE_RENDERING_INPUT_INVALID,
                message,
                details
        );
    }
}
