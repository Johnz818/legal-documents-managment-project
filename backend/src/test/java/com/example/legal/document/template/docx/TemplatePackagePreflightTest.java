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
                "[Content_Types].xml", contentTypes(),
                "_rels/.rels", relationships(false),
                "word/document.xml", "document",
                "../outside.xml", "unsafe"
        )), TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);
        assertFailure(zip(Map.of(
                "[Content_Types].xml", contentTypes(),
                "_rels/.rels", relationships(false),
                "word/document.xml", "document",
                "word\\unsafe.xml", "unsafe"
        )), TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);

        byte[] duplicate = duplicateEntryZip();
        assertFailure(duplicate, TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);
    }

    @Test
    void rejectsExternalRelationshipsAndMissingCoreParts() {
        byte[] externalRelationship = zip(Map.of(
                "[Content_Types].xml", contentTypes(),
                "_rels/.rels", relationships(true),
                "word/document.xml", "document"
        ));
        assertThatThrownBy(() -> preflight().validate(externalRelationship))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE);
                    assertThat(exception.getDetails())
                            .containsEntry("feature", "EXTERNAL_RELATIONSHIP");
                });
        assertFailure(zip(Map.of(
                "[Content_Types].xml", contentTypes(),
                "_rels/.rels", relationships(false)
        )), TemplateInspectionErrorCode.TEMPLATE_FILE_NOT_DOCX);
    }

    @Test
    void rejectsEmbeddedAndActiveContentButPermitsOrdinaryImages() {
        assertPackageFeatureFailure(
                "word/embeddings/embedded.xlsx",
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED,
                "EMBEDDED_OBJECT"
        );
        assertPackageFeatureFailure(
                "word/activeX/activeX1.bin",
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                "ACTIVEX"
        );
        assertPackageFeatureFailure(
                "word/vbaData.xml",
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                "MACRO"
        );
        assertThatCode(() -> preflight().validate(packageWith("word/media/logo.png")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDeniedSemanticsAtUnconventionalPackagePaths() {
        assertSemanticFeatureFailure(
                relationships(false, "http://schemas.microsoft.com/office/2006/relationships/vbaProject"),
                contentTypes(),
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                "MACRO"
        );
        assertSemanticFeatureFailure(
                relationships(false, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/package"),
                contentTypes(),
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSUPPORTED,
                "EMBEDDED_OBJECT"
        );
        assertSemanticFeatureFailure(
                relationships(false),
                contentTypes("application/vnd.ms-office.activeX"),
                TemplateInspectionErrorCode.TEMPLATE_PACKAGE_UNSAFE,
                "ACTIVEX"
        );
    }

    @Test
    void permitsUnknownInternalRelationshipSemanticsDuringPreflight() {
        assertThatCode(() -> preflight().validate(zip(Map.of(
                "[Content_Types].xml", contentTypes(),
                "_rels/.rels", relationships(false, "urn:example:internal-extension"),
                "word/document.xml", "document"
        )))).doesNotThrowAnyException();
    }

    @Test
    void enforcesEntryCountEntrySizeExpandedSizeAndCompressionRatio() {
        byte[] twoEntries = zip(Map.of("one", "1", "two", "2"));
        assertLimitFailure(twoEntries, new TemplatePackageLimits(
                1000, 1, 100, 100, 100, 0
        ));

        byte[] validShape = zip(new LinkedHashMap<>(Map.of(
                "[Content_Types].xml", contentTypes(),
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

    private void assertPackageFeatureFailure(
            String entryName,
            TemplateInspectionErrorCode expectedCode,
            String expectedFeature
    ) {
        assertThatThrownBy(() -> preflight().validate(packageWith(entryName)))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(expectedCode);
                    assertThat(exception.getDetails()).containsEntry("feature", expectedFeature);
                });
    }

    private void assertSemanticFeatureFailure(
            String relationshipXml,
            String contentTypeXml,
            TemplateInspectionErrorCode expectedCode,
            String expectedFeature
    ) {
        byte[] content = zip(Map.of(
                "[Content_Types].xml", contentTypeXml,
                "_rels/.rels", relationshipXml,
                "word/document.xml", "document",
                "word/assets/renamed.dat", "content"
        ));
        assertThatThrownBy(() -> preflight().validate(content))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(expectedCode);
                    assertThat(exception.getDetails()).containsEntry("feature", expectedFeature);
                });
    }

    private byte[] packageWith(String additionalEntry) {
        return zip(Map.of(
                "[Content_Types].xml", contentTypes(),
                "_rels/.rels", relationships(false),
                "word/document.xml", "document",
                additionalEntry, "content"
        ));
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
        return relationships(external, "urn:test:relationship");
    }

    private String relationships(boolean external, String type) {
        String targetMode = external ? " TargetMode=\"External\"" : "";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="%s" Target="word/assets/renamed.dat"%s/>
                </Relationships>
                """.formatted(type, targetMode);
    }

    private String contentTypes(String... additionalContentTypes) {
        StringBuilder declarations = new StringBuilder();
        for (int index = 0; index < additionalContentTypes.length; index++) {
            declarations.append("<Override PartName=\"/word/assets/part")
                    .append(index)
                    .append(".dat\" ContentType=\"")
                    .append(additionalContentTypes[index])
                    .append("\"/>");
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="xml" ContentType="application/xml"/>
                  %s
                </Types>
                """.formatted(declarations);
    }
}
