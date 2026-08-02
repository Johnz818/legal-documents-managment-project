package com.example.legal.document.template.publication;

import java.util.List;

public record TemplatePublicationCommand(
        String name,
        String description,
        String originalFileName,
        String contentType,
        byte[] content,
        List<TemplateFieldDefinition> fields
) {
    public TemplatePublicationCommand {
        content = content == null ? null : content.clone();
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
