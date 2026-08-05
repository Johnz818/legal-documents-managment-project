package com.example.legal.document.generation.api;

import com.example.legal.document.generation.DocumentGenerationCommand;
import com.example.legal.document.generation.DocumentGenerationException;
import com.example.legal.document.generation.DocumentGenerationService;
import com.example.legal.document.generation.GeneratedDocument;
import com.example.legal.document.generation.GenerationErrorCode;
import com.example.legal.document.generation.GenerationPreparation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentGenerationControllerTest {

    private DocumentGenerationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(DocumentGenerationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentGenerationController(service))
                .setControllerAdvice(new DocumentGenerationExceptionHandler())
                .build();
    }

    @Test
    void preparesGenerationInputs() throws Exception {
        when(service.prepare(7L, 11L, 2, "Asia/Shanghai"))
                .thenReturn(new GenerationPreparation(7L, 11L, 2, "Asia/Shanghai", List.of()));

        mockMvc.perform(get("/api/cases/7/document-generations/preparation")
                        .param("templateId", "11")
                        .param("versionNumber", "2")
                        .param("timezone", "Asia/Shanghai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(7))
                .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"));
    }

    @Test
    void generatesWithRequiredIdempotencyHeader() throws Exception {
        String key = UUID.randomUUID().toString();
        when(service.generate(any(DocumentGenerationCommand.class))).thenReturn(new GeneratedDocument(
                55L, 7L, 11L, 2, 44L, true, "generated.docx", LocalDateTime.of(2026, 8, 5, 10, 0)
        ));

        mockMvc.perform(post("/api/cases/7/document-generations")
                        .param("templateId", "11")
                        .param("versionNumber", "2")
                        .param("timezone", "Asia/Shanghai")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"values":[{
                                  "fieldKey":"case_number",
                                  "value":"(2026)沪0115民初1001号",
                                  "valueSource":"CASE_FIELD"
                                }]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generationId").value(55))
                .andExpect(jsonPath("$.outputAvailable").value(true));
    }

    @Test
    void invalidJsonAndMissingHeaderReturnControlledProblem() throws Exception {
        mockMvc.perform(post("/api/cases/7/document-generations")
                        .param("templateId", "11")
                        .param("versionNumber", "2")
                        .param("timezone", "Asia/Shanghai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GENERATION_REQUEST_INVALID"));

        mockMvc.perform(post("/api/cases/7/document-generations")
                        .param("templateId", "11")
                        .param("versionNumber", "2")
                        .param("timezone", "Asia/Shanghai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GENERATION_REQUEST_INVALID"));

        mockMvc.perform(get("/api/cases/7/document-generations/preparation")
                        .param("templateId", "11")
                        .param("versionNumber", "not-a-number")
                        .param("timezone", "Asia/Shanghai"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GENERATION_REQUEST_INVALID"));
    }

    @Test
    void domainFailureUsesStableCodeAndDetails() throws Exception {
        when(service.prepare(7L, 11L, 2, "Asia/Shanghai")).thenThrow(new DocumentGenerationException(
                GenerationErrorCode.GENERATION_VALUE_STALE,
                HttpStatus.CONFLICT,
                "A value is stale",
                java.util.Map.of("fieldKey", "case_number")
        ));

        mockMvc.perform(get("/api/cases/7/document-generations/preparation")
                        .param("templateId", "11")
                        .param("versionNumber", "2")
                        .param("timezone", "Asia/Shanghai"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GENERATION_VALUE_STALE"))
                .andExpect(jsonPath("$.details.fieldKey").value("case_number"));
    }
}
