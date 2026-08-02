package com.example.legal.document.template.inspection;

import org.springframework.http.HttpStatus;

import java.util.Map;

public final class TemplateInspectionException extends RuntimeException {

    private final TemplateInspectionErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public TemplateInspectionException(
            TemplateInspectionErrorCode code,
            HttpStatus status,
            String message
    ) {
        this(code, status, message, Map.of(), null);
    }

    public TemplateInspectionException(
            TemplateInspectionErrorCode code,
            HttpStatus status,
            String message,
            Map<String, Object> details
    ) {
        this(code, status, message, details, null);
    }

    public TemplateInspectionException(
            TemplateInspectionErrorCode code,
            HttpStatus status,
            String message,
            Throwable cause
    ) {
        this(code, status, message, Map.of(), cause);
    }

    private TemplateInspectionException(
            TemplateInspectionErrorCode code,
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

    public TemplateInspectionErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
