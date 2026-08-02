package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplatePackagePreflightTest {

    private static final TemplatePackageLimits DEFAULT_LIMITS = new TemplatePackageLimits(
            5 * 1024 * 1024,
            2048,
            10 * 1024 * 1024,
            50 * 1024 * 1024,
            100,
            1024 * 1024
    );

    @Test
    void acceptsAValidDocxPackage() {
        assertThatCode(() -> preflight().validate(SyntheticDocx.paragraphs("{{案号}}")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyOversizedAndNonDocxContent() {
        assertFailure(new byte[0], TemplateInspectionErrorCode.TEMPLATE_FILE_EMPTY);
        TemplatePackagePreflight tinyLimit = new TemplatePackagePreflight(new TemplatePackageLimits(
                2, 10, 100, 100, 100, 0
        ));
        assertThatThrownBy(() -> tinyLimit.validate(new byte[]{1, 2, 3}))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(TemplateInspectionErrorCode.TEMPLATE_FILE_TOO_LARGE));
        assertFailure("not a zip".getBytes(), TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX);
    }

    @Test
    void rejectsUnsafeEntryNamesAndDuplicateNames() {
        assertFailure(zip(Map.of(
                "[Content_Types].xml", "types",
                "_rels/.rels", relationships(false),
                "word/document.xml", "document",
                "../outside.xml", "unsafe"
        )), TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);
        assertFailure(zip(Map.of(
                "[Content_Types].xml", "types",
                "_rels/.rels", relationships(false),
                "word/document.xml", "document",
                "word\\unsafe.xml", "unsafe"
        )), TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);

        byte[] duplicate = duplicateEntryZip();
        assertFailure(duplicate, TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);
    }

    @Test
    void rejectsExternalRelationshipsAndMissingCoreParts() {
        assertFailure(zip(Map.of(
                "[Content_Types].xml", "types",
                "_rels/.rels", relationships(true),
                "word/document.xml", "document"
        )), TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);
        assertFailure(zip(Map.of(
                "[Content_Types].xml", "types",
                "_rels/.rels", relationships(false)
        )), TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX);
    }

    @Test
    void enforcesEntryCountEntrySizeExpandedSizeAndCompressionRatio() {
        byte[] twoEntries = zip(Map.of("one", "1", "two", "2"));
        assertLimitFailure(twoEntries, new TemplatePackageLimits(
                1000, 1, 100, 100, 100, 0
        ));

        byte[] validShape = zip(new LinkedHashMap<>(Map.of(
                "[Content_Types].xml", "12345",
                "_rels/.rels", relationships(false),
                "word/document.xml", "document"
        )));
        assertLimitFailure(validShape, new TemplatePackageLimits(
                1000, 10, 4, 100, 100, 0
        ));
        assertLimitFailure(validShape, new TemplatePackageLimits(
                1000, 10, 100, 8, 100, 0
        ));

        byte[] compressed = zip(Map.of(
                "[Content_Types].xml", "a".repeat(5000),
                "_rels/.rels", relationships(false),
                "word/document.xml", "b".repeat(5000)
        ));
        assertLimitFailure(compressed, new TemplatePackageLimits(
                50_000, 10, 10_000, 20_000, 2, 1
        ));
    }

    private TemplatePackagePreflight preflight() {
        return new TemplatePackagePreflight(DEFAULT_LIMITS);
    }

    private void assertFailure(byte[] content, TemplateInspectionErrorCode expected) {
        assertThatThrownBy(() -> preflight().validate(content))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(expected));
    }

    private void assertLimitFailure(byte[] content, TemplatePackageLimits limits) {
        assertThatThrownBy(() -> new TemplatePackagePreflight(limits).validate(content))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE));
    }

    private byte[] duplicateEntryZip() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                write(zip, "duplicate.xml", "one");
                // Java's writer rejects exact duplicates, so use case variation; preflight is case-insensitive.
                write(zip, "DUPLICATE.XML", "two");
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] zip(Map<String, String> entries) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    write(zip, entry.getKey(), entry.getValue());
                }
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void write(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes());
        zip.closeEntry();
    }

    private String relationships(boolean external) {
        String targetMode = external ? " TargetMode=\"External\"" : "";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="test" Target="word/document.xml"%s/>
                </Relationships>
                """.formatted(targetMode);
    }
}
