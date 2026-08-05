package com.example.legal.document.generation.api;

import java.util.List;

public record DocumentGenerationRequest(List<GenerationValueRequest> values) {
}
