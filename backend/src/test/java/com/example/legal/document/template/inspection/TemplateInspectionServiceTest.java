package com.example.legal.document.template.inspection;

import com.example.legal.document.template.docx.TemplatePackageLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateInspectionServiceTest {

    @Mock
    private TemplateDocumentInspector inspector;

    private TemplateInspectionService service;

    @BeforeEach
    void setUp() {
        service = new TemplateInspectionService(inspector, new TemplatePackageLimits(
                100, 10, 100, 100, 100, 0
        ));
    }

    @Test
    void delegatesValidatedDocxBytesToInspector() {
        byte[] content = {1, 2, 3};
        MockMultipartFile file = file("template.docx", content);
        TemplateInspection expected = new TemplateInspection(List.of(
                new DetectedTemplateMarker(TemplateMarkerKind.CHINESE, "案号", 1)
        ));
        when(inspector.inspect(content)).thenReturn(expected);

        assertThat(service.inspect(file)).isSameAs(expected);
        verify(inspector).inspect(content);
    }

    @Test
    void rejectsMissingEmptyOversizedAndInvalidFilenamesBeforeInspection() {
        assertFailure(null, TemplateInspectionErrorCode.TEMPLATE_FILE_EMPTY);
        assertFailure(file("template.docx", new byte[0]), TemplateInspectionErrorCode.TEMPLATE_FILE_EMPTY);
        assertFailure(file("template.docx", new byte[101]), TemplateInspectionErrorCode.TEMPLATE_FILE_TOO_LARGE);
        assertFailure(file("template.pdf", new byte[]{1}), TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX);
        assertFailure(file("../template.docx", new byte[]{1}), TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX);
        assertFailure(file("bad\nname.docx", new byte[]{1}), TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX);
        assertFailure(file("x".repeat(251) + ".docx", new byte[]{1}),
                TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX);
        verify(inspector, never()).inspect(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void convertsMultipartReadFailureToControlledInspectionFailure() throws IOException {
        MockMultipartFile file = org.mockito.Mockito.mock(MockMultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1L);
        when(file.getOriginalFilename()).thenReturn("template.docx");
        when(file.getBytes()).thenThrow(new IOException("test read failure"));

        assertThatThrownBy(() -> service.inspect(file))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED);
                    assertThat(exception).hasCauseInstanceOf(IOException.class);
                });
    }

    private void assertFailure(
            MockMultipartFile file,
            TemplateInspectionErrorCode expectedCode
    ) {
        assertThatThrownBy(() -> service.inspect(file))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(expectedCode));
    }

    private MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }
}
