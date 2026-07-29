package com.example.legal.document.upload;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentUploadExceptionHandlerTest {

    private final DocumentUploadExceptionHandler handler =
            new DocumentUploadExceptionHandler();

    @Test
    void mapsInvalidUploadToBadRequest() {
        assertThat(handler.handleInvalidUpload().getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsMissingCaseToNotFound() {
        assertThat(handler.handleMissingCase().getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsDomainAndMultipartSizeFailuresToPayloadTooLarge() {
        assertThat(handler.handleOversizedUpload(new DocumentTooLargeException()).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(handler.handleOversizedUpload(
                new MaxUploadSizeExceededException(5L)
        ).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void mapsUnsupportedContentToUnsupportedMediaType() {
        assertThat(handler.handleUnsupportedType().getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
