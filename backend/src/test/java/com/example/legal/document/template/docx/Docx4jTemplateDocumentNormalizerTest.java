package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Docx4jTemplateDocumentNormalizerTest {

    private final TemplatePackagePreflight preflight = new TemplatePackagePreflight(
            new TemplatePackageLimits(5_000_000, 2048, 10_000_000, 50_000_000, 100, 1_000_000)
    );
    private final Docx4jTemplateDocumentNormalizer normalizer =
            new Docx4jTemplateDocumentNormalizer(preflight);
    private final Docx4jTemplateDocumentInspector inspector =
            new Docx4jTemplateDocumentInspector(preflight);

    @Test
    void normalizesRepeatedAndSplitRunMarkersAndPreservesSurroundingText() throws Exception {
        byte[] source = SyntheticDocx.create(wordPackage ->
                wordPackage.getMainDocumentPart().getContent().add(
                        SyntheticDocx.paragraph("前缀 {{案", "号}} / {{案号}} 后缀")
                ));

        byte[] result = normalizer.normalize(source, Map.of(
                new TemplateMarker(TemplateMarkerKind.CHINESE, "案号"), "case_number"
        ));

        assertThat(inspector.inspect(result).markers())
                .singleElement()
                .satisfies(marker -> {
                    assertThat(marker.kind()).isEqualTo(TemplateMarkerKind.CANONICAL);
                    assertThat(marker.value()).isEqualTo("case_number");
                    assertThat(marker.occurrenceCount()).isEqualTo(2);
                });
        WordprocessingMLPackage loaded = WordprocessingMLPackage.load(new ByteArrayInputStream(result));
        assertThat(loaded.getMainDocumentPart().getXML()).contains("前缀", "后缀");
    }

    @Test
    void leavesAnEmptyTemplateUnchangedAsAValidDocx() {
        byte[] result = normalizer.normalize(SyntheticDocx.paragraphs("固定正文"), Map.of());

        assertThat(result).isNotEmpty();
        assertThat(inspector.inspect(result).markers()).isEmpty();
    }
}
