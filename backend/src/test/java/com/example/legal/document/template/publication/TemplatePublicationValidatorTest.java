package com.example.legal.document.template.publication;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.inspection.DetectedTemplateMarker;
import com.example.legal.document.template.inspection.TemplateInspection;
import com.example.legal.document.template.inspection.TemplateMarker;
import com.example.legal.document.template.inspection.TemplateMarkerKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplatePublicationValidatorTest {

    private final TemplatePublicationValidator validator = new TemplatePublicationValidator();

    @Test
    void acceptsManyToOneChineseGroupingAndCanonicalSelfMapping() {
        TemplateInspection inspection = inspection(
                marker(TemplateMarkerKind.CHINESE, "案号"),
                marker(TemplateMarkerKind.CHINESE, "案件编号"),
                marker(TemplateMarkerKind.CANONICAL, "court_name")
        );
        List<TemplateFieldDefinition> fields = List.of(
                field("case_number", DocumentFieldValueType.TEXT,
                        DocumentFieldDefaultSource.CASE_FIELD, "caseNumber",
                        new TemplateMarker(TemplateMarkerKind.CHINESE, "案号"),
                        new TemplateMarker(TemplateMarkerKind.CHINESE, "案件编号")),
                field("court_name", DocumentFieldValueType.TEXT,
                        DocumentFieldDefaultSource.CASE_FIELD, "courtName",
                        new TemplateMarker(TemplateMarkerKind.CANONICAL, "court_name"))
        );

        assertThat(validator.validate(inspection, fields)).hasSize(3);
        validator.validateCanonicalResult(inspection(
                marker(TemplateMarkerKind.CANONICAL, "case_number"),
                marker(TemplateMarkerKind.CANONICAL, "court_name")
        ), fields);
    }

    @Test
    void acceptsEmptyContractAndAllApprovedSourceCombinations() {
        assertThat(validator.validate(new TemplateInspection(List.of()), List.of())).isEmpty();
        validator.validate(inspection(
                marker(TemplateMarkerKind.CHINESE, "日期"),
                marker(TemplateMarkerKind.CHINESE, "备注")
        ), List.of(
                field("current_date", DocumentFieldValueType.DATE,
                        DocumentFieldDefaultSource.SYSTEM_VALUE, "currentDate",
                        new TemplateMarker(TemplateMarkerKind.CHINESE, "日期")),
                field("note", DocumentFieldValueType.BOOLEAN,
                        DocumentFieldDefaultSource.USER_INPUT, null,
                        new TemplateMarker(TemplateMarkerKind.CHINESE, "备注"))
        ));
    }

    @Test
    void rejectsMissingExtraDuplicateAndWrongCanonicalMappings() {
        TemplateInspection inspection = inspection(marker(TemplateMarkerKind.CHINESE, "案号"));
        TemplateFieldDefinition valid = field("case_number", DocumentFieldValueType.TEXT,
                DocumentFieldDefaultSource.CASE_FIELD, "caseNumber",
                new TemplateMarker(TemplateMarkerKind.CHINESE, "案号"));

        assertInvalid(() -> validator.validate(inspection, List.of()));
        assertInvalid(() -> validator.validate(inspection, List.of(
                valid,
                field("other", DocumentFieldValueType.TEXT, DocumentFieldDefaultSource.USER_INPUT, null,
                        new TemplateMarker(TemplateMarkerKind.CHINESE, "案号"))
        )));
        assertInvalid(() -> validator.validate(inspection, List.of(
                field("case_number", DocumentFieldValueType.TEXT, DocumentFieldDefaultSource.CASE_FIELD,
                        "caseNumber", new TemplateMarker(TemplateMarkerKind.CHINESE, "不存在"))
        )));
        assertInvalid(() -> validator.validate(
                inspection(marker(TemplateMarkerKind.CANONICAL, "case_number")),
                List.of(field("other", DocumentFieldValueType.TEXT,
                        DocumentFieldDefaultSource.USER_INPUT, null,
                        new TemplateMarker(TemplateMarkerKind.CANONICAL, "case_number")))
        ));
    }

    @Test
    void rejectsInvalidMetadataAndSourceTypeContracts() {
        TemplateMarker marker = new TemplateMarker(TemplateMarkerKind.CHINESE, "案号");
        TemplateInspection inspection = inspection(marker(TemplateMarkerKind.CHINESE, "案号"));
        assertInvalid(() -> validator.validate(inspection, List.of(
                new TemplateFieldDefinition("Bad-Key", "案号", null, DocumentFieldValueType.TEXT,
                        true, DocumentFieldDefaultSource.USER_INPUT, null, List.of(marker))
        )));
        assertInvalid(() -> validator.validate(inspection, List.of(
                field("case_number", DocumentFieldValueType.DATE,
                        DocumentFieldDefaultSource.CASE_FIELD, "caseNumber", marker)
        )));
        assertInvalid(() -> validator.validate(inspection, List.of(
                field("case_number", DocumentFieldValueType.TEXT,
                        DocumentFieldDefaultSource.USER_INPUT, "caseNumber", marker)
        )));
        assertInvalid(() -> validator.validate(inspection, List.of(
                field("case_number", DocumentFieldValueType.TEXT,
                        DocumentFieldDefaultSource.CASE_FIELD, null, marker)
        )));
    }

    @Test
    void rejectsCanonicalRescanMismatchOrRemainingChineseMarker() {
        List<TemplateFieldDefinition> fields = List.of(field(
                "case_number", DocumentFieldValueType.TEXT,
                DocumentFieldDefaultSource.USER_INPUT, null,
                new TemplateMarker(TemplateMarkerKind.CHINESE, "案号")
        ));
        assertInvalid(() -> validator.validateCanonicalResult(new TemplateInspection(List.of()), fields));
        assertInvalid(() -> validator.validateCanonicalResult(
                inspection(marker(TemplateMarkerKind.CHINESE, "案号")), fields
        ));
    }

    private TemplateFieldDefinition field(
            String key,
            DocumentFieldValueType type,
            DocumentFieldDefaultSource source,
            String sourceKey,
            TemplateMarker... markers
    ) {
        return new TemplateFieldDefinition(
                key, "显示名", null, type, true, source, sourceKey, List.of(markers)
        );
    }

    private TemplateInspection inspection(DetectedTemplateMarker... markers) {
        return new TemplateInspection(List.of(markers));
    }

    private DetectedTemplateMarker marker(TemplateMarkerKind kind, String value) {
        return new DetectedTemplateMarker(kind, value, 1);
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(TemplatePublicationException.class);
    }
}
