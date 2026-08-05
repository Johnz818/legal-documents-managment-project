package com.example.legal.document.generation;

import java.util.Objects;

public record ReviewedGenerationValue(
        String fieldKey,
        String value,
        GenerationValueSource valueSource
) {

    public ReviewedGenerationValue {
        Objects.requireNonNull(fieldKey, "fieldKey must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(valueSource, "valueSource must not be null");
    }
}
