package com.example.legal.document.template.publication;

import org.springframework.http.HttpStatus;

import java.util.Map;

public final class TemplatePublicationException extends RuntimeException {

    private final TemplatePublicationErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public TemplatePublicationException(
            TemplatePublicationErrorCode code,
            HttpStatus status,
            String message
    ) {
        this(code, status, message, Map.of(), null);
    }

    public TemplatePublicationException(
            TemplatePublicationErrorCode code,
            HttpStatus status,
            String message,
            Map<String, Object> details
    ) {
        this(code, status, message, details, null);
    }

    public TemplatePublicationException(
            TemplatePublicationErrorCode code,
            HttpStatus status,
            String message,
            Throwable cause
    ) {
        this(code, status, message, Map.of(), cause);
    }

    private TemplatePublicationException(
            TemplatePublicationErrorCode code,
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

    public TemplatePublicationErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
