package com.example.legal.document.template.api;

import com.example.legal.document.template.inspection.DetectedTemplateMarker;
import com.example.legal.document.template.inspection.TemplateInspection;
import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import com.example.legal.document.template.inspection.TemplateInspectionService;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TemplateInspectionControllerTest {

    private TemplateInspectionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TemplateInspectionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TemplateInspectionController(service))
                .setControllerAdvice(new TemplateApiExceptionHandler())
                .build();
    }

    @Test
    void returnsDetectedMarkers() throws Exception {
        when(service.inspect(any())).thenReturn(new TemplateInspection(List.of(
                new DetectedTemplateMarker(TemplateMarkerKind.CHINESE, "案号", 2),
                new DetectedTemplateMarker(TemplateMarkerKind.CANONICAL, "court_name", 1)
        )));

        mockMvc.perform(multipart("/api/document-templates/inspections")
                        .file(file()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.markers[0].kind").value("CHINESE"))
                .andExpect(jsonPath("$.markers[0].value").value("案号"))
                .andExpect(jsonPath("$.markers[0].occurrenceCount").value(2))
                .andExpect(jsonPath("$.markers[1].kind").value("CANONICAL"));
    }

    @Test
    void returnsStructuredInspectionFailure() throws Exception {
        when(service.inspect(any())).thenThrow(new TemplateInspectionException(
                TemplateInspectionErrorCode.TEMPLATE_MARKER_UNSUPPORTED_LOCATION,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Template markers are not supported in header",
                Map.of("location", "HEADER")
        ));

        mockMvc.perform(multipart("/api/document-templates/inspections").file(file()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid document template"))
                .andExpect(jsonPath("$.code").value("TEMPLATE_MARKER_UNSUPPORTED_LOCATION"))
                .andExpect(jsonPath("$.details.location").value("HEADER"));
    }

    @Test
    void returnsStructuredErrorWhenFilePartIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/document-templates/inspections"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEMPLATE_FILE_EMPTY"));
    }

    private MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1}
        );
    }
}
