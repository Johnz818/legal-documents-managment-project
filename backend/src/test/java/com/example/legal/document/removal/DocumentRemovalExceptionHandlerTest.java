package com.example.legal.document.removal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentRemovalExceptionHandlerTest {

    @Test
    void mapsMissingDocumentToNotFound() {
        DocumentRemovalExceptionHandler handler =
                new DocumentRemovalExceptionHandler();

        assertThat(handler.handleNotFound().getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
