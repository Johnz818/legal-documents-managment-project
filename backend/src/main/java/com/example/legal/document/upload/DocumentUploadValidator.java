package com.example.legal.document.upload;

import com.example.legal.document.DocumentFormat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DocumentUploadValidator {

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] DOC_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final int MAXIMUM_DOCX_ENTRY_COUNT = 10_000;
    private static final Map<String, SupportedDocument> SUPPORTED_DOCUMENTS = Map.of(
            "pdf", new SupportedDocument(DocumentFormat.PDF, Set.of("application/pdf")),
            "doc", new SupportedDocument(DocumentFormat.DOC, Set.of("application/msword")),
            "docx", new SupportedDocument(
                    DocumentFormat.DOCX,
                    Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            )
    );

    private final long maximumFileSize;

    public DocumentUploadValidator(long maximumFileSize) {
        if (maximumFileSize <= 0) {
            throw new IllegalArgumentException("maximumFileSize must be positive");
        }
        this.maximumFileSize = maximumFileSize;
    }

    public ValidatedDocumentUpload validate(DocumentUploadCommand command) {
        if (command == null || command.contentSource() == null) {
            throw new InvalidDocumentUploadException("Document content is required");
        }
        if (command.contentLength() <= 0) {
            throw new InvalidDocumentUploadException("Document must not be empty");
        }
        if (command.contentLength() > maximumFileSize) {
            throw new DocumentTooLargeException();
        }

        String fileName = validateFileName(command.originalFileName());
        String extension = extensionOf(fileName);
        SupportedDocument supportedDocument = SUPPORTED_DOCUMENTS.get(extension);
        if (supportedDocument == null
                || command.contentType() == null
                || !supportedDocument.contentTypes().contains(command.contentType().toLowerCase(Locale.ROOT))) {
            throw new UnsupportedDocumentTypeException();
        }

        verifyContent(command, supportedDocument.format());
        return new ValidatedDocumentUpload(
                fileName,
                supportedDocument.format(),
                command.contentType().toLowerCase(Locale.ROOT),
                command.contentLength()
        );
    }

    private String validateFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new InvalidDocumentUploadException("File name is required");
        }
        String fileName = originalFileName.trim();
        if (fileName.length() > 255
                || fileName.indexOf('/') >= 0
                || fileName.indexOf('\\') >= 0
                || fileName.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidDocumentUploadException("File name is invalid");
        }
        return fileName;
    }

    private String extensionOf(String fileName) {
        int separator = fileName.lastIndexOf('.');
        if (separator <= 0 || separator == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private void verifyContent(DocumentUploadCommand command, DocumentFormat format) {
        try (InputStream content = command.contentSource().openStream()) {
            boolean valid = switch (format) {
                case PDF -> hasSignature(content, PDF_SIGNATURE);
                case DOC -> hasSignature(content, DOC_SIGNATURE);
                case DOCX -> isSafeDocx(content);
            };
            if (!valid) {
                throw new UnsupportedDocumentTypeException();
            }
        } catch (IOException exception) {
            throw new DocumentUploadProcessingException("Unable to inspect document content", exception);
        }
    }

    private boolean hasSignature(InputStream content, byte[] signature) throws IOException {
        return java.util.Arrays.equals(content.readNBytes(signature.length), signature);
    }

    private boolean isSafeDocx(InputStream content) throws IOException {
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        int entryCount = 0;

        try (ZipInputStream zip = new ZipInputStream(content)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAXIMUM_DOCX_ENTRY_COUNT || "word/vbaProject.bin".equals(entry.getName())) {
                    return false;
                }
                hasContentTypes |= "[Content_Types].xml".equals(entry.getName());
                hasDocument |= "word/document.xml".equals(entry.getName());
            }
        }
        return hasContentTypes && hasDocument;
    }

    private record SupportedDocument(DocumentFormat format, Set<String> contentTypes) {
    }
}
