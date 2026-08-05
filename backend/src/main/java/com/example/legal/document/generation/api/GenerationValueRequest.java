package com.example.legal.document.generation.api;

import com.example.legal.document.generation.GenerationValueSource;
import com.example.legal.document.generation.ReviewedGenerationValue;

public record GenerationValueRequest(
        String fieldKey,
        String value,
        GenerationValueSource valueSource
) {

    ReviewedGenerationValue toValue() {
        return new ReviewedGenerationValue(fieldKey, value, valueSource);
    }
}
