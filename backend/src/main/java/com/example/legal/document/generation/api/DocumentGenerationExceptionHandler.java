package com.example.legal.document.generation.api;

import com.example.legal.document.generation.DocumentGenerationException;
import com.example.legal.document.generation.GenerationErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = DocumentGenerationController.class)
public class DocumentGenerationExceptionHandler {

    @ExceptionHandler(DocumentGenerationException.class)
    ProblemDetail handleGenerationFailure(DocumentGenerationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle("Document generation failed");
        problem.setProperty("code", exception.getCode().name());
        if (!exception.getDetails().isEmpty()) {
            problem.setProperty("details", exception.getDetails());
        }
        return problem;
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    ProblemDetail handleInvalidHttpRequest() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Generation request is invalid"
        );
        problem.setTitle("Document generation failed");
        problem.setProperty("code", GenerationErrorCode.GENERATION_REQUEST_INVALID.name());
        return problem;
    }
}
