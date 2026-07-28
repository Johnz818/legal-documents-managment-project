package com.example.legal.document.upload;

import com.example.legal.document.DocumentFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentUploadValidatorTest {

    private static final long FIVE_MEGABYTES = 5L * 1024 * 1024;
    private final DocumentUploadValidator validator = new DocumentUploadValidator(FIVE_MEGABYTES);

    @Test
    void acceptsSupportedPdfDocument() {
        byte[] content = "%PDF-1.7 content".getBytes();

        ValidatedDocumentUpload result = validator.validate(command(
                " pleading.PDF ",
                "application/pdf",
                content
        ));

        assertThat(result.originalFileName()).isEqualTo("pleading.PDF");
        assertThat(result.fileFormat()).isEqualTo(DocumentFormat.PDF);
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.contentLength()).isEqualTo(content.length);
    }

    @Test
    void acceptsSupportedLegacyWordDocument() {
        byte[] content = {
                (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0
        };

        assertThat(validator.validate(command("pleading.doc", "application/msword", content)).fileFormat())
                .isEqualTo(DocumentFormat.DOC);
    }

    @Test
    void acceptsSafeDocxDocument() throws IOException {
        byte[] content = docx("[Content_Types].xml", "word/document.xml");

        assertThat(validator.validate(command(
                "pleading.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                content
        )).fileFormat()).isEqualTo(DocumentFormat.DOCX);
    }

    @Test
    void rejectsMissingCommandOrContent() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidDocumentUploadException.class);
        assertThatThrownBy(() -> validator.validate(
                new DocumentUploadCommand("file.pdf", "application/pdf", 1, null)
        )).isInstanceOf(InvalidDocumentUploadException.class);
    }

    @Test
    void rejectsEmptyOrOversizedDocument() {
        assertThatThrownBy(() -> validator.validate(command("file.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(InvalidDocumentUploadException.class);
        assertThatThrownBy(() -> validator.validate(new DocumentUploadCommand(
                "file.pdf",
                "application/pdf",
                FIVE_MEGABYTES + 1,
                () -> new ByteArrayInputStream("%PDF-".getBytes())
        ))).isInstanceOf(DocumentTooLargeException.class);
    }

    @Test
    void rejectsMissingOrUnsafeFileName() {
        byte[] content = "%PDF-".getBytes();

        assertThatThrownBy(() -> validator.validate(command(" ", "application/pdf", content)))
                .isInstanceOf(InvalidDocumentUploadException.class);
        assertThatThrownBy(() -> validator.validate(command("../file.pdf", "application/pdf", content)))
                .isInstanceOf(InvalidDocumentUploadException.class);
        assertThatThrownBy(() -> validator.validate(command("folder\\file.pdf", "application/pdf", content)))
                .isInstanceOf(InvalidDocumentUploadException.class);
        assertThatThrownBy(() -> validator.validate(command("file\u0000.pdf", "application/pdf", content)))
                .isInstanceOf(InvalidDocumentUploadException.class);
        assertThatThrownBy(() -> validator.validate(command("a".repeat(252) + ".pdf", "application/pdf", content)))
                .isInstanceOf(InvalidDocumentUploadException.class);
    }

    @Test
    void rejectsUnsupportedExtensionOrMediaType() {
        byte[] content = "%PDF-".getBytes();

        assertThatThrownBy(() -> validator.validate(command("file", "application/pdf", content)))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
        assertThatThrownBy(() -> validator.validate(command("file.txt", "text/plain", content)))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
        assertThatThrownBy(() -> validator.validate(command("file.pdf", null, content)))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
        assertThatThrownBy(() -> validator.validate(command("file.pdf", "application/msword", content)))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void rejectsContentThatDoesNotMatchDeclaredFormat() {
        assertThatThrownBy(() -> validator.validate(command(
                "file.pdf",
                "application/pdf",
                "not a pdf".getBytes()
        ))).isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void rejectsOrdinaryZipAndMacroEnabledDocx() throws IOException {
        String docxContentType =
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        assertThatThrownBy(() -> validator.validate(command(
                "file.docx",
                docxContentType,
                docx("unrelated.txt")
        ))).isInstanceOf(UnsupportedDocumentTypeException.class);
        assertThatThrownBy(() -> validator.validate(command(
                "file.docx",
                docxContentType,
                docx("[Content_Types].xml", "word/document.xml", "word/vbaProject.bin")
        ))).isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void normalizesContentInspectionFailure() {
        DocumentUploadCommand command = new DocumentUploadCommand(
                "file.pdf",
                "application/pdf",
                5,
                () -> {
                    throw new IOException("test failure");
                }
        );

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(DocumentUploadProcessingException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void requiresPositiveConfiguredLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DocumentUploadValidator(0));
    }

    private DocumentUploadCommand command(String fileName, String contentType, byte[] content) {
        return new DocumentUploadCommand(
                fileName,
                contentType,
                content.length,
                () -> new ByteArrayInputStream(content)
        );
    }

    private byte[] docx(String... entryNames) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (String entryName : entryNames) {
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write("<xml/>".getBytes());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
