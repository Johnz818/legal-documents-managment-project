package com.example.legal.document.generation;

import java.util.Objects;

public record GenerationValueToPersist(
        Long templateFieldId,
        String resolvedValue,
        GenerationValueSource valueSource
) {

    public GenerationValueToPersist {
        Objects.requireNonNull(templateFieldId, "templateFieldId must not be null");
        Objects.requireNonNull(resolvedValue, "resolvedValue must not be null");
        Objects.requireNonNull(valueSource, "valueSource must not be null");
    }
}
