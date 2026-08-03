package com.example.legal.document.generation;

import java.util.Map;

public final class TemplateRenderingException extends RuntimeException {

    private final TemplateRenderingErrorCode code;
    private final Map<String, Object> details;

    public TemplateRenderingException(
            TemplateRenderingErrorCode code,
            String message,
            Map<String, Object> details
    ) {
        this(code, message, details, null);
    }

    public TemplateRenderingException(
            TemplateRenderingErrorCode code,
            String message,
            Throwable cause
    ) {
        this(code, message, Map.of(), cause);
    }

    private TemplateRenderingException(
            TemplateRenderingErrorCode code,
            String message,
            Map<String, Object> details,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public TemplateRenderingErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
