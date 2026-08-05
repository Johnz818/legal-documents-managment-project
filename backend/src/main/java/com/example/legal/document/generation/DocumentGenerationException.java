package com.example.legal.document.generation;

import org.springframework.http.HttpStatus;

import java.util.Map;

public final class DocumentGenerationException extends RuntimeException {

    private final GenerationErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public DocumentGenerationException(GenerationErrorCode code, HttpStatus status, String message) {
        this(code, status, message, Map.of(), null);
    }

    public DocumentGenerationException(
            GenerationErrorCode code,
            HttpStatus status,
            String message,
            Map<String, Object> details
    ) {
        this(code, status, message, details, null);
    }

    public DocumentGenerationException(
            GenerationErrorCode code,
            HttpStatus status,
            String message,
            Throwable cause
    ) {
        this(code, status, message, Map.of(), cause);
    }

    private DocumentGenerationException(
            GenerationErrorCode code,
            HttpStatus status,
            String message,
            Map<String, Object> details,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    public GenerationErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
