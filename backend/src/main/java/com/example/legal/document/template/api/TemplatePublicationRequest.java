package com.example.legal.document.template.api;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import com.example.legal.document.template.publication.TemplateFieldDefinition;
import com.example.legal.document.template.publication.TemplatePublicationErrorCode;
import com.example.legal.document.template.publication.TemplatePublicationException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.springframework.http.HttpStatus;

import java.util.List;

public record TemplatePublicationRequest(String name, String description, List<FieldRequest> fields) {

    @JsonAnySetter
    void rejectUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("Unknown template publication property: " + name);
    }

    public List<TemplateFieldDefinition> toFields() {
        return toFields(fields);
    }

    static List<TemplateFieldDefinition> toFields(List<FieldRequest> fields) {
        if (fields == null) {
            return List.of();
        }
        if (fields.stream().anyMatch(java.util.Objects::isNull)) {
            throw invalid();
        }
        return fields.stream().map(FieldRequest::toDefinition).toList();
    }

    public record FieldRequest(
            String fieldKey,
            String displayName,
            String description,
            DocumentFieldValueType valueType,
            boolean required,
            DocumentFieldDefaultSource defaultSource,
            String sourceKey,
            List<MarkerRequest> markers
    ) {
        @JsonAnySetter
        void rejectUnknownProperty(String name, Object value) {
            throw new IllegalArgumentException("Unknown template field property: " + name);
        }

        TemplateFieldDefinition toDefinition() {
            if (markers != null && markers.stream().anyMatch(java.util.Objects::isNull)) {
                throw invalid();
            }
            List<TemplateMarker> converted = markers == null ? List.of()
                    : markers.stream().map(MarkerRequest::toMarker).toList();
            return new TemplateFieldDefinition(
                    fieldKey, displayName, description, valueType, required,
                    defaultSource, sourceKey, converted
            );
        }
    }

    public record MarkerRequest(TemplateMarkerKind kind, String value) {
        @JsonAnySetter
        void rejectUnknownProperty(String name, Object unknownValue) {
            throw new IllegalArgumentException("Unknown template marker property: " + name);
        }

        TemplateMarker toMarker() {
            if (kind == null || value == null) {
                throw invalid();
            }
            return new TemplateMarker(kind, value);
        }
    }

    private static TemplatePublicationException invalid() {
        return new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_PUBLICATION_INVALID,
                HttpStatus.BAD_REQUEST,
                "Publication request is invalid"
        );
    }
}
