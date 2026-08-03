package com.example.legal.document.template.api;

import com.example.legal.document.template.DocumentTemplateType;
import com.example.legal.document.template.publication.PublishedTemplateVersion;
import com.example.legal.document.template.publication.TemplateDownload;
import com.example.legal.document.template.publication.TemplatePage;
import com.example.legal.document.template.publication.TemplatePublicationErrorCode;
import com.example.legal.document.template.publication.TemplatePublicationException;
import com.example.legal.document.template.publication.TemplatePublicationService;
import com.example.legal.document.template.publication.TemplateSummary;
import com.example.legal.document.template.publication.TemplateVersionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TemplatePublicationControllerTest {

    private TemplatePublicationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TemplatePublicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TemplatePublicationController(service))
                .setControllerAdvice(new TemplateApiExceptionHandler())
                .build();
    }

    @Test
    void createsAndPublishesMultipartVersions() throws Exception {
        when(service.create(any())).thenReturn(published(1));
        when(service.publishVersion(eq(7L), any())).thenReturn(published(2));

        mockMvc.perform(multipart("/api/document-templates").file(file()).file(publication()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(1));
        mockMvc.perform(multipart("/api/document-templates/7/versions")
                        .file(file()).file(versionPublication()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2));
    }

    @Test
    void listsAndRetrievesTemplateResources() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 1, 0);
        when(service.listTemplates(0, 20)).thenReturn(new TemplatePage<>(List.of(
                new TemplateSummary(7L, "函", null, DocumentTemplateType.CUSTOM, now, now)
        ), 0, 20, 1, 1));
        when(service.listVersions(7L, 0, 20)).thenReturn(new TemplatePage<>(List.of(
                new TemplateVersionSummary(1, "函.docx", "application/test", 3, "a".repeat(64), now)
        ), 0, 20, 1, 1));
        when(service.getVersion(7L, 1)).thenReturn(published(1));

        mockMvc.perform(get("/api/document-templates"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(7));
        mockMvc.perform(get("/api/document-templates/7/versions"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].versionNumber").value(1));
        mockMvc.perform(get("/api/document-templates/7/versions/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.templateId").value(7));
    }

    @Test
    void downloadsExactPublishedContent() throws Exception {
        when(service.download(7L, 1)).thenReturn(new TemplateDownload(
                "律师函.docx", "application/octet-stream", 3,
                new ByteArrayInputStream(new byte[]{1, 2, 3})
        ));

        mockMvc.perform(get("/api/document-templates/7/versions/1/content"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Length", "3"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void returnsStructuredPublicationFailure() throws Exception {
        when(service.getVersion(7L, 99)).thenThrow(new TemplatePublicationException(
                TemplatePublicationErrorCode.TEMPLATE_VERSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Template resource was not found"
        ));

        mockMvc.perform(get("/api/document-templates/7/versions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEMPLATE_VERSION_NOT_FOUND"));
    }

    @Test
    void rejectsNullFieldElementsAsStructuredBadRequest() throws Exception {
        MockMultipartFile invalidPublication = new MockMultipartFile(
                "publication", "publication.json", "application/json",
                "{\"name\":\"函\",\"fields\":[null]}".getBytes()
        );

        mockMvc.perform(multipart("/api/document-templates")
                        .file(file()).file(invalidPublication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEMPLATE_PUBLICATION_INVALID"));
    }

    @Test
    void rejectsTemplateMetadataOnLaterVersionPublication() throws Exception {
        mockMvc.perform(multipart("/api/document-templates/7/versions")
                        .file(file()).file(publication()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEMPLATE_PUBLICATION_INVALID"));
    }

    @Test
    void rejectsUnknownMetadataOnTemplateCreation() throws Exception {
        MockMultipartFile invalidPublication = new MockMultipartFile(
                "publication", "publication.json", "application/json",
                "{\"name\":\"函\",\"descripton\":\"拼写错误\",\"fields\":[]}".getBytes()
        );

        mockMvc.perform(multipart("/api/document-templates")
                        .file(file()).file(invalidPublication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEMPLATE_PUBLICATION_INVALID"));
    }

    @Test
    void rejectsUnknownNestedFieldAndMarkerProperties() throws Exception {
        assertInvalidPublication("""
                {"name":"函","fields":[{"fieldKey":"case_number","descripton":"拼写错误"}]}
                """);
        assertInvalidPublication("""
                {"name":"函","fields":[{"fieldKey":"case_number","markers":[
                  {"kind":"CANONICAL","value":"case_number","vale":"拼写错误"}
                ]}]}
                """);
    }

    private void assertInvalidPublication(String json) throws Exception {
        MockMultipartFile invalidPublication = new MockMultipartFile(
                "publication", "publication.json", "application/json", json.getBytes()
        );
        mockMvc.perform(multipart("/api/document-templates")
                        .file(file()).file(invalidPublication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEMPLATE_PUBLICATION_INVALID"));
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "template.docx", "application/test", new byte[]{1});
    }

    private MockMultipartFile publication() {
        return new MockMultipartFile(
                "publication", "publication.json", "application/json",
                "{\"name\":\"函\",\"fields\":[]}".getBytes()
        );
    }

    private MockMultipartFile versionPublication() {
        return new MockMultipartFile(
                "publication", "publication.json", "application/json",
                "{\"fields\":[]}".getBytes()
        );
    }

    private PublishedTemplateVersion published(int version) {
        return new PublishedTemplateVersion(
                7L, "函", null, version, "函.docx", "application/test",
                3, "a".repeat(64), LocalDateTime.of(2026, 8, 3, 1, 0), List.of()
        );
    }
}
