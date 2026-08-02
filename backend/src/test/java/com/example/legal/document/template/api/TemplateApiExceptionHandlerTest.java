package com.example.legal.document.template.api;

import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateApiExceptionHandlerTest {

    private final TemplateApiExceptionHandler handler = new TemplateApiExceptionHandler();

    @Test
    void mapsMultipartLimitFailureToTemplateProblem() {
        var problem = handler.handleOversizedMultipart();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problem.getProperties()).containsEntry(
                "code",
                TemplateInspectionErrorCode.TEMPLATE_FILE_TOO_LARGE.name()
        );
    }

    @Test
    void mapsMissingMultipartPartToTemplateProblem() {
        var problem = handler.handleMissingMultipartPart();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry(
                "code",
                TemplateInspectionErrorCode.TEMPLATE_FILE_EMPTY.name()
        );
    }
}
