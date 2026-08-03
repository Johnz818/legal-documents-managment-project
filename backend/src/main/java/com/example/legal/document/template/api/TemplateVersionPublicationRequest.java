package com.example.legal.document.template.api;

import com.example.legal.document.template.publication.TemplateFieldDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record TemplateVersionPublicationRequest(
        List<TemplatePublicationRequest.FieldRequest> fields
) {
    public List<TemplateFieldDefinition> toFields() {
        return TemplatePublicationRequest.toFields(fields);
    }

    @JsonAnySetter
    void rejectUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("Unknown version publication property: " + name);
    }
}
