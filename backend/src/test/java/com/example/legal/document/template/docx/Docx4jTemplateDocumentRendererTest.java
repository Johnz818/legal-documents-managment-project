package com.example.legal.document.template.docx;

import com.example.legal.document.generation.TemplateRenderingErrorCode;
import com.example.legal.document.generation.TemplateRenderingException;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtContentBlock;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Docx4jTemplateDocumentRendererTest {

    private final TemplatePackagePreflight preflight = new TemplatePackagePreflight(
            new TemplatePackageLimits(5_000_000, 2048, 10_000_000, 50_000_000, 100, 1_000_000)
    );
    private final Docx4jTemplateDocumentRenderer renderer = new Docx4jTemplateDocumentRenderer(
            new Docx4jTemplateDocumentInspector(preflight)
    );

    @Test
    void rendersChineseScalarAcrossRunsAndTablesWhilePreservingBasicFormatting() throws Exception {
        byte[] template = SyntheticDocx.create(wordPackage -> {
            P paragraph = SyntheticDocx.paragraph("案号：", "{{case_", "number}}；再次：{{case_number}}");
            R markerFirstRun = (R) paragraph.getContent().get(1);
            RPr runProperties = new RPr();
            runProperties.setB(new BooleanDefaultTrue());
            markerFirstRun.setRPr(runProperties);
            wordPackage.getMainDocumentPart().addObject(paragraph);
            wordPackage.getMainDocumentPart().addObject(tableWithCell(
                    SyntheticDocx.paragraph("法院：{{court_name}}")
            ));
        });

        byte[] rendered = renderer.render(template, Map.of(
                "case_number", " (2026)沪0115民初1001号 ",
                "court_name", "上海市浦东新区人民法院"
        ));

        WordprocessingMLPackage result = WordprocessingMLPackage.load(new ByteArrayInputStream(rendered));
        String xml = result.getMainDocumentPart().getXML();
        P renderedParagraph = (P) XmlUtils.unwrap(result.getMainDocumentPart().getContent().getFirst());
        R renderedMarkerRun = (R) XmlUtils.unwrap(renderedParagraph.getContent().get(1));
        assertThat(xml)
                .contains("(2026)沪0115民初1001号", "上海市浦东新区人民法院", "xml:space=\"preserve\"")
                .doesNotContain("{{case_number}}", "{{court_name}}");
        assertThat(renderedMarkerRun.getRPr()).isNotNull();
        assertThat(renderedMarkerRun.getRPr().getB()).isNotNull();
    }

    @Test
    void preservesOrdinaryImages() throws Exception {
        byte[] template = SyntheticDocx.create(wordPackage -> {
            SyntheticDocx.addImageParagraph(wordPackage);
            wordPackage.getMainDocumentPart().addParagraphOfText("律师：{{lawyer_name}}");
        });

        byte[] rendered = renderer.render(template, Map.of("lawyer_name", "张律师"));

        WordprocessingMLPackage result = WordprocessingMLPackage.load(new ByteArrayInputStream(rendered));
        assertThat(result.getParts().getParts().values())
                .anyMatch(BinaryPartAbstractImage.class::isInstance);
        assertThat(result.getMainDocumentPart().getXML()).contains("张律师");
    }

    @Test
    void acceptsEmptyValuesAndAPlaceholderFreeTemplate() throws Exception {
        byte[] renderedValue = renderer.render(
                SyntheticDocx.paragraphs("备注：[{{note}}]"),
                Map.of("note", "")
        );
        byte[] renderedEmptyTemplate = renderer.render(
                SyntheticDocx.paragraphs("固定正文"),
                Map.of()
        );

        WordprocessingMLPackage valueResult = WordprocessingMLPackage.load(
                new ByteArrayInputStream(renderedValue)
        );
        assertThat(valueResult.getMainDocumentPart().getXML()).contains("备注：[]");
        assertThat(renderedEmptyTemplate).isNotEmpty();
    }

    @Test
    void rejectsMissingAndExtraValuesWithDeterministicDetails() {
        byte[] template = SyntheticDocx.paragraphs("{{case_number}} / {{court_name}}");

        assertThatThrownBy(() -> renderer.render(template, Map.of(
                "case_number", "(2026)沪0115民初1001号",
                "unused", "不应使用"
        )))
                .isInstanceOfSatisfying(TemplateRenderingException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(TemplateRenderingErrorCode.TEMPLATE_RENDERING_INPUT_INVALID);
                    assertThat(exception.getDetails()).containsEntry("missingKeys", java.util.List.of("court_name"));
                    assertThat(exception.getDetails()).containsEntry("extraKeys", java.util.List.of("unused"));
                });
    }

    @Test
    void rejectsNullAndMultilineValuesButDoesNotRejectMarkerLikeLiteralText() throws Exception {
        byte[] template = SyntheticDocx.paragraphs("{{description}}");
        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("description", null);

        assertThatThrownBy(() -> renderer.render(template, nullValue))
                .isInstanceOfSatisfying(TemplateRenderingException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(TemplateRenderingErrorCode.TEMPLATE_RENDERING_INPUT_INVALID));
        assertThatThrownBy(() -> renderer.render(template, Map.of("description", "第一行\n第二行")))
                .isInstanceOfSatisfying(TemplateRenderingException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(TemplateRenderingErrorCode.TEMPLATE_RENDERING_INPUT_INVALID);
                    assertThat(exception.getDetails()).containsEntry("fieldKey", "description");
                });

        byte[] rendered = renderer.render(template, Map.of("description", "引用 {{article_1}}"));
        WordprocessingMLPackage result = WordprocessingMLPackage.load(new ByteArrayInputStream(rendered));
        assertThat(result.getMainDocumentPart().getXML()).contains("引用 {{article_1}}");
    }

    @Test
    void rejectsNonCanonicalAndUnsafeTemplatesThroughTheRenderingBoundary() {
        assertThatThrownBy(() -> renderer.render(
                SyntheticDocx.paragraphs("{{案号}}"),
                Map.of("case_number", "(2026)沪0115民初1001号")
        )).isInstanceOfSatisfying(TemplateRenderingException.class, exception ->
                assertThat(exception.getCode())
                        .isEqualTo(TemplateRenderingErrorCode.TEMPLATE_RENDERING_TEMPLATE_UNSUPPORTED));

        assertThatThrownBy(() -> renderer.render(new byte[]{1, 2, 3}, Map.of()))
                .isInstanceOfSatisfying(TemplateRenderingException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(TemplateRenderingErrorCode.TEMPLATE_RENDERING_TEMPLATE_UNSUPPORTED);
                    assertThat(exception.getDetails()).containsKey("inspectionCode");
                });
    }

    @Test
    void rejectsCanonicalMarkersInUnsupportedContentControlsWithLocationDetails() {
        byte[] template = SyntheticDocx.create(wordPackage ->
                wordPackage.getMainDocumentPart().addObject(contentControl("{{case_number}}"))
        );

        assertThatThrownBy(() -> renderer.render(
                template,
                Map.of("case_number", "(2026)沪0115民初1001号")
        )).isInstanceOfSatisfying(TemplateRenderingException.class, exception -> {
            assertThat(exception.getCode())
                    .isEqualTo(TemplateRenderingErrorCode.TEMPLATE_RENDERING_TEMPLATE_UNSUPPORTED);
            assertThat(exception.getDetails()).containsEntry(
                    "inspectionCode",
                    "TEMPLATE_MARKER_UNSUPPORTED_LOCATION"
            );
            assertThat(exception.getDetails().get("inspectionDetails"))
                    .isEqualTo(Map.of(
                            "location", "CONTENT_CONTROL",
                            "markerKind", "CANONICAL",
                            "markerValue", "case_number"
                    ));
        });
    }

    private static SdtBlock contentControl(String text) {
        SdtContentBlock content = new SdtContentBlock();
        content.getContent().add(SyntheticDocx.paragraph(text));
        SdtBlock block = new SdtBlock();
        block.setSdtContent(content);
        return block;
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
