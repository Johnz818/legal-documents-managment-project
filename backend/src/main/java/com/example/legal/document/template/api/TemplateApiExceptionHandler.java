package com.example.legal.document.template.api;

import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = TemplateInspectionController.class)
public class TemplateApiExceptionHandler {

    @ExceptionHandler(TemplateInspectionException.class)
    ProblemDetail handleInspectionFailure(TemplateInspectionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.getStatus(),
                exception.getMessage()
        );
        problem.setTitle("Invalid document template");
        problem.setProperty("code", exception.getCode().name());
        if (!exception.getDetails().isEmpty()) {
            problem.setProperty("details", exception.getDetails());
        }
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleOversizedMultipart() {
        return problem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                TemplateInspectionErrorCode.TEMPLATE_FILE_TOO_LARGE,
                "Template file exceeds the configured size limit"
        );
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    ProblemDetail handleMissingMultipartPart() {
        return problem(
                HttpStatus.BAD_REQUEST,
                TemplateInspectionErrorCode.TEMPLATE_FILE_EMPTY,
                "Template file is required"
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            TemplateInspectionErrorCode code,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("Invalid document template");
        problem.setProperty("code", code.name());
        return problem;
    }
}
