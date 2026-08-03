package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.DetectedTemplateMarker;
import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.Hdr;
import org.docx4j.wml.CTSdtContentRun;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtContentBlock;
import org.docx4j.wml.SdtRun;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Docx4jTemplateDocumentInspectorTest {

    private static final TemplatePackageLimits LIMITS = new TemplatePackageLimits(
            5 * 1024 * 1024,
            2048,
            10 * 1024 * 1024,
            50 * 1024 * 1024,
            100,
            1024 * 1024
    );
    private final Docx4jTemplateDocumentInspector inspector =
            new Docx4jTemplateDocumentInspector(new TemplatePackagePreflight(LIMITS));

    @Test
    void returnsUniqueChineseAndCanonicalMarkersInFirstOccurrenceOrder() {
        byte[] document = SyntheticDocx.paragraphs(
                "案号：{{案号}}，法院：{{court_name}}",
                "再次引用：{{案号}}"
        );

        assertThat(inspector.inspect(document).markers()).containsExactly(
                new DetectedTemplateMarker(TemplateMarkerKind.CHINESE, "案号", 2),
                new DetectedTemplateMarker(TemplateMarkerKind.CANONICAL, "court_name", 1)
        );
    }

    @Test
    void detectsMarkersSplitAcrossFormattingRuns() {
        byte[] document = SyntheticDocx.create(wordPackage ->
                wordPackage.getMainDocumentPart().addObject(
                        SyntheticDocx.paragraph("案号：{{案", "号}}")
                ));

        assertThat(inspector.inspect(document).markers()).containsExactly(
                new DetectedTemplateMarker(TemplateMarkerKind.CHINESE, "案号", 1)
        );
    }

    @Test
    void traversesTablesAndNestedTablesInDocumentOrder() {
        byte[] document = SyntheticDocx.create(wordPackage -> {
            Tbl outer = tableWithCell(SyntheticDocx.paragraph("{{案号}}"));
            Tc outerCell = (Tc) ((Tr) outer.getContent().getFirst()).getContent().getFirst();
            outerCell.getContent().add(tableWithCell(SyntheticDocx.paragraph("{{court_name}}")));
            wordPackage.getMainDocumentPart().addObject(outer);
        });

        assertThat(inspector.inspect(document).markers()).containsExactly(
                new DetectedTemplateMarker(TemplateMarkerKind.CHINESE, "案号", 1),
                new DetectedTemplateMarker(TemplateMarkerKind.CANONICAL, "court_name", 1)
        );
    }

    @Test
    void acceptsAnEmptyMarkerContract() {
        assertThat(inspector.inspect(SyntheticDocx.paragraphs("固定合同文本")).markers()).isEmpty();
    }

    @Test
    void rejectsMalformedMarkersInSupportedParagraphs() {
        assertThatThrownBy(() -> inspector.inspect(SyntheticDocx.paragraphs("{{case-number}}")))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(TemplateInspectionErrorCode.TEMPLATE_MARKER_INVALID));
    }

    @Test
    void rejectsMarkersInHeadersWithLocationDetails() {
        byte[] document = SyntheticDocx.create(wordPackage -> {
            HeaderPart header = new HeaderPart();
            Hdr contents = new Hdr();
            contents.getContent().add(SyntheticDocx.paragraph("{{案号}}"));
            header.setJaxbElement(contents);
            Relationship relationship = wordPackage.getMainDocumentPart().addTargetPart(header);
            assertThat(relationship).isNotNull();
        });

        assertThatThrownBy(() -> inspector.inspect(document))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(
                            TemplateInspectionErrorCode.TEMPLATE_MARKER_UNSUPPORTED_LOCATION
                    );
                    assertThat(exception.getDetails()).containsEntry("location", "HEADER");
                });
    }

    @Test
    void rejectsMarkersInsideContentControlsWithLocationDetails() {
        byte[] document = SyntheticDocx.create(wordPackage ->
                wordPackage.getMainDocumentPart().addObject(contentControl("{{案号}}"))
        );

        assertThatThrownBy(() -> inspector.inspect(document))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(
                            TemplateInspectionErrorCode.TEMPLATE_MARKER_UNSUPPORTED_LOCATION
                    );
                    assertThat(exception.getDetails())
                            .containsEntry("location", "CONTENT_CONTROL")
                            .containsEntry("markerKind", "CHINESE")
                            .containsEntry("markerValue", "案号");
                });
    }

    @Test
    void rejectsCanonicalMarkersInsideRunLevelContentControls() {
        byte[] document = SyntheticDocx.create(wordPackage -> {
            org.docx4j.wml.P paragraph = new org.docx4j.wml.P();
            paragraph.getContent().add(runContentControl("{{case_number}}"));
            wordPackage.getMainDocumentPart().addObject(paragraph);
        });

        assertThatThrownBy(() -> inspector.inspect(document))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(
                            TemplateInspectionErrorCode.TEMPLATE_MARKER_UNSUPPORTED_LOCATION
                    );
                    assertThat(exception.getDetails())
                            .containsEntry("location", "CONTENT_CONTROL")
                            .containsEntry("markerKind", "CANONICAL")
                            .containsEntry("markerValue", "case_number");
                });
    }

    @Test
    void acceptsAGenuineImageBearingDocx() {
        byte[] document = SyntheticDocx.create(SyntheticDocx::addImageParagraph);

        assertThat(inspector.inspect(document).markers()).isEmpty();
    }

    @Test
    void permitsFixedBoilerplateInsideContentControls() {
        byte[] document = SyntheticDocx.create(wordPackage ->
                wordPackage.getMainDocumentPart().addObject(contentControl("固定正文"))
        );

        assertThat(inspector.inspect(document).markers()).isEmpty();
    }

    private static SdtBlock contentControl(String text) {
        SdtContentBlock content = new SdtContentBlock();
        content.getContent().add(SyntheticDocx.paragraph(text));
        SdtBlock block = new SdtBlock();
        block.setSdtContent(content);
        return block;
    }

    private static SdtRun runContentControl(String text) {
        CTSdtContentRun content = new CTSdtContentRun();
        content.getContent().add(SyntheticDocx.paragraph(text).getContent().getFirst());
        SdtRun run = new SdtRun();
        run.setSdtContent(content);
        return run;
    }

    private static Tbl tableWithCell(Object content) {
        Tc cell = new Tc();
        cell.getContent().add(content);
        Tr row = new Tr();
        row.getContent().add(cell);
        Tbl table = new Tbl();
        table.getContent().add(row);
        return table;
    }
}
