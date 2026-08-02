package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateInspectionErrorCode;
import com.example.legal.document.template.inspection.TemplateInspectionException;
import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateMarkerParserTest {

    private final TemplateMarkerParser parser = new TemplateMarkerParser();

    @Test
    void parsesChineseAndCanonicalMarkersInOrder() {
        assertThat(parser.parse("案号{{案号}}，法院{{court_name}}", "MAIN_BODY"))
                .containsExactly(
                        new TemplateMarker(TemplateMarkerKind.CHINESE, "案号"),
                        new TemplateMarker(TemplateMarkerKind.CANONICAL, "court_name")
                );
    }

    @Test
    void acceptsFortyHanCodePointsAndRejectsLongerValues() {
        String forty = "案".repeat(40);
        String fortyOne = "案".repeat(41);

        assertThat(parser.parse("{{" + forty + "}}", "MAIN_BODY")).hasSize(1);
        assertInvalid("{{" + fortyOne + "}}");
    }

    @Test
    void rejectsMalformedAndUnmatchedBraceCandidates() {
        assertInvalid("{{}}");
        assertInvalid("{{ 案号 }}");
        assertInvalid("{{case-number}}");
        assertInvalid("{{CaseNumber}}");
        assertInvalid("{{案号1}}");
        assertInvalid("{{{案号}}}");
        assertInvalid("{{案号");
        assertInvalid("案号}}");
    }

    @Test
    void ignoresOrdinarySingleBraces() {
        assertThat(parser.parse("合同条款 {案号} 无标记", "MAIN_BODY")).isEmpty();
    }

    private void assertInvalid(String text) {
        assertThatThrownBy(() -> parser.parse(text, "MAIN_BODY"))
                .isInstanceOfSatisfying(TemplateInspectionException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo(TemplateInspectionErrorCode.TEMPLATE_MARKER_INVALID);
                    assertThat(exception.getDetails()).containsEntry("location", "MAIN_BODY");
                });
    }
}
