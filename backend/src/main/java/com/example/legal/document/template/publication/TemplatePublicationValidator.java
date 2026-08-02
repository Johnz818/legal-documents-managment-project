package com.example.legal.document.template.publication;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.inspection.TemplateInspection;
import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public final class TemplatePublicationValidator {

    private static final Pattern FIELD_KEY = Pattern.compile("[a-z][a-z0-9_]{0,99}");
    private static final Map<String, DocumentFieldValueType> CASE_SOURCES = Map.ofEntries(
            Map.entry("caseNumber", DocumentFieldValueType.TEXT),
            Map.entry("caseName", DocumentFieldValueType.TEXT),
            Map.entry("courtName", DocumentFieldValueType.TEXT),
            Map.entry("caseCause", DocumentFieldValueType.TEXT),
            Map.entry("plaintiff", DocumentFieldValueType.TEXT),
            Map.entry("defendant", DocumentFieldValueType.TEXT),
            Map.entry("leadLawyerName", DocumentFieldValueType.TEXT),
            Map.entry("description", DocumentFieldValueType.TEXT),
            Map.entry("filingDate", DocumentFieldValueType.DATE),
            Map.entry("hearingDate", DocumentFieldValueType.DATE),
            Map.entry("judgmentDate", DocumentFieldValueType.DATE)
    );
    private static final Map<String, DocumentFieldValueType> SYSTEM_SOURCES = Map.of(
            "currentDate", DocumentFieldValueType.DATE
    );

    public Map<TemplateMarker, String> validate(
            TemplateInspection inspection,
            List<TemplateFieldDefinition> fields
    ) {
        Set<String> keys = new HashSet<>();
        Map<TemplateMarker, String> mappings = new LinkedHashMap<>();
        Set<TemplateMarker> detected = new HashSet<>();
        inspection.markers().forEach(marker -> detected.add(
                new TemplateMarker(marker.kind(), marker.value())
        ));

        for (TemplateFieldDefinition field : fields) {
            validateField(field);
            if (!keys.add(field.fieldKey())) {
                throw invalid("Field keys must be unique within a template version");
            }
            if (field.markers().isEmpty()) {
                throw mappingInvalid("Every field must be backed by at least one detected marker");
            }
            for (TemplateMarker marker : field.markers()) {
                if (!detected.contains(marker)) {
                    throw mappingInvalid("Field mapping contains a marker not found in the DOCX");
                }
                if (mappings.putIfAbsent(marker, field.fieldKey()) != null) {
                    throw mappingInvalid("A detected marker may belong to only one field");
                }
                if (marker.kind() == TemplateMarkerKind.CANONICAL
                        && !marker.value().equals(field.fieldKey())) {
                    throw mappingInvalid("A canonical marker must map to its own field key");
                }
            }
        }
        if (!mappings.keySet().equals(detected)) {
            throw mappingInvalid("Every detected marker must be mapped exactly once");
        }
        if (detected.isEmpty() && !fields.isEmpty()) {
            throw mappingInvalid("A template without markers must have an empty field contract");
        }
        return Map.copyOf(mappings);
    }

    public void validateCanonicalResult(TemplateInspection inspection, List<TemplateFieldDefinition> fields) {
        Map<String, Integer> detected = new HashMap<>();
        inspection.markers().forEach(marker -> {
            if (marker.kind() != TemplateMarkerKind.CANONICAL) {
                throw new TemplatePublicationException(
                        TemplatePublicationErrorCode.TEMPLATE_NORMALIZATION_FAILED,
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Normalized template still contains non-canonical markers"
                );
            }
            detected.put(marker.value(), marker.occurrenceCount());
        });
        Set<String> expected = new HashSet<>();
        fields.forEach(field -> expected.add(field.fieldKey()));
        if (!detected.keySet().equals(expected)) {
            throw new TemplatePublicationException(
                    TemplatePublicationErrorCode.TEMPLATE_NORMALIZATION_FAILED,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Normalized template markers do not match the confirmed field contract"
            );
        }
    }

    private void validateField(TemplateFieldDefinition field) {
        if (field == null
                || field.fieldKey() == null
                || !FIELD_KEY.matcher(field.fieldKey()).matches()
                || field.displayName() == null
                || field.displayName().isBlank()
                || field.displayName().length() > 200
                || field.description() != null && field.description().length() > 1000
                || field.valueType() == null
                || field.defaultSource() == null) {
            throw invalid("Template field definition is invalid");
        }
        validateSource(field);
    }

    private void validateSource(TemplateFieldDefinition field) {
        DocumentFieldValueType expected;
        if (field.defaultSource() == DocumentFieldDefaultSource.USER_INPUT) {
            if (field.sourceKey() != null) {
                throw sourceInvalid("USER_INPUT fields must not define a source key");
            }
            return;
        }
        if (field.sourceKey() == null || field.sourceKey().isBlank()) {
            throw sourceInvalid("Case and system fields require a source key");
        }
        expected = field.defaultSource() == DocumentFieldDefaultSource.CASE_FIELD
                ? CASE_SOURCES.get(field.sourceKey())
                : SYSTEM_SOURCES.get(field.sourceKey());
        if (expected == null || expected != field.valueType()) {
            throw sourceInvalid("Source key is unknown or incompatible with the field value type");
        }
    }

    private TemplatePublicationException invalid(String message) {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_PUBLICATION_INVALID,
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private TemplatePublicationException mappingInvalid(String message) {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_MARKER_MAPPING_INVALID,
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private TemplatePublicationException sourceInvalid(String message) {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_FIELD_SOURCE_INVALID,
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
