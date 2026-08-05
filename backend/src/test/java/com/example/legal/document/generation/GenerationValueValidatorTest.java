package com.example.legal.document.generation;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.DocumentTemplateFieldEntity;
import com.example.legal.legalcase.CaseEntity;
import com.example.legal.legalcase.CaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationValueValidatorTest {

    private GenerationValueValidator validator;
    private CaseEntity legalCase;

    @BeforeEach
    void setUp() {
        GenerationValueResolver resolver = new GenerationValueResolver(
                Clock.fixed(Instant.parse("2026-08-04T16:30:00Z"), ZoneId.of("UTC"))
        );
        validator = new GenerationValueValidator(resolver);
        legalCase = new CaseEntity(
                "(2026)沪0115民初1001号", "合同纠纷", CaseStatus.IN_TRIAL,
                "张三", "某公司", "李律师"
        );
    }

    @Test
    void acceptsChineseDateAsSemanticMatchForDeterministicDate() {
        DocumentTemplateFieldEntity field = field(
                "current_date", DocumentFieldValueType.DATE,
                DocumentFieldDefaultSource.SYSTEM_VALUE, "currentDate", true
        );

        List<GenerationValueValidator.ValidatedGenerationValue> result = validator.validate(
                List.of(field),
                List.of(new ReviewedGenerationValue(
                        "current_date", "2026-08-05", GenerationValueSource.SYSTEM_VALUE
                )),
                legalCase,
                ZoneId.of("Asia/Shanghai")
        );

        assertThat(result).singleElement().satisfies(value ->
                assertThat(value.value()).isEqualTo("2026-08-05")
        );
    }

    @Test
    void rejectsStaleDeterministicValueWithoutDisclosingExpectedValue() {
        DocumentTemplateFieldEntity field = field(
                "case_number", DocumentFieldValueType.TEXT,
                DocumentFieldDefaultSource.CASE_FIELD, "caseNumber", true
        );

        assertThatThrownBy(() -> validator.validate(
                List.of(field),
                List.of(new ReviewedGenerationValue(
                        "case_number", "旧案号", GenerationValueSource.CASE_FIELD
                )),
                legalCase,
                ZoneId.of("Asia/Shanghai")
        )).isInstanceOfSatisfying(DocumentGenerationException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(GenerationErrorCode.GENERATION_VALUE_STALE);
            assertThat(exception.getDetails()).containsEntry("fieldKey", "case_number");
            assertThat(exception.getMessage()).doesNotContain(legalCase.getCaseNumber());
        });
    }

    @Test
    void acceptsManualCorrectionWithoutMatchingDefault() {
        DocumentTemplateFieldEntity field = field(
                "case_number", DocumentFieldValueType.TEXT,
                DocumentFieldDefaultSource.CASE_FIELD, "caseNumber", true
        );

        assertThat(validator.validate(
                List.of(field),
                List.of(new ReviewedGenerationValue(
                        "case_number", "人工修正", GenerationValueSource.USER_INPUT
                )),
                legalCase,
                ZoneId.of("Asia/Shanghai")
        )).singleElement().satisfies(value -> assertThat(value.value()).isEqualTo("人工修正"));
    }

    @Test
    void rejectsMissingDuplicateExtraMultilineOversizedAndInvalidScalarValues() {
        DocumentTemplateFieldEntity text = field(
                "text", DocumentFieldValueType.TEXT,
                DocumentFieldDefaultSource.USER_INPUT, null, true
        );
        DocumentTemplateFieldEntity decimal = field(
                "amount", DocumentFieldValueType.DECIMAL,
                DocumentFieldDefaultSource.USER_INPUT, null, true
        );

        assertInvalid(List.of(text), List.of());
        assertInvalid(List.of(text), List.of(
                manual("text", "one"), manual("text", "two")
        ));
        assertInvalid(List.of(text), List.of(manual("extra", "value")));
        assertInvalid(List.of(text), List.of(manual("text", "line one\nline two")));
        assertInvalid(List.of(text), List.of(manual("text", "x".repeat(10_001))));
        assertInvalid(List.of(decimal), List.of(manual("amount", "1e6")));
    }

    @Test
    void validatesBooleanDateAndOptionalEmptyContracts() {
        DocumentTemplateFieldEntity bool = field(
                "confirmed", DocumentFieldValueType.BOOLEAN,
                DocumentFieldDefaultSource.USER_INPUT, null, true
        );
        DocumentTemplateFieldEntity date = field(
                "date", DocumentFieldValueType.DATE,
                DocumentFieldDefaultSource.USER_INPUT, null, true
        );
        DocumentTemplateFieldEntity optional = field(
                "optional", DocumentFieldValueType.DECIMAL,
                DocumentFieldDefaultSource.USER_INPUT, null, false
        );

        assertThat(validator.validate(
                List.of(bool, date, optional),
                List.of(manual("confirmed", "false"), manual("date", "2024年2月29日"), manual("optional", "")),
                legalCase,
                ZoneId.of("Asia/Shanghai")
        )).hasSize(3);
        assertInvalid(List.of(bool), List.of(manual("confirmed", "TRUE")));
        assertInvalid(List.of(date), List.of(manual("date", "2025年2月29日")));
    }

    private void assertInvalid(
            List<DocumentTemplateFieldEntity> fields,
            List<ReviewedGenerationValue> values
    ) {
        assertThatThrownBy(() -> validator.validate(
                fields, values, legalCase, ZoneId.of("Asia/Shanghai")
        )).isInstanceOfSatisfying(DocumentGenerationException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(GenerationErrorCode.GENERATION_REQUEST_INVALID)
        );
    }

    private ReviewedGenerationValue manual(String key, String value) {
        return new ReviewedGenerationValue(key, value, GenerationValueSource.USER_INPUT);
    }

    private DocumentTemplateFieldEntity field(
            String key,
            DocumentFieldValueType type,
            DocumentFieldDefaultSource source,
            String sourceKey,
            boolean required
    ) {
        return new DocumentTemplateFieldEntity(
                1L, key, key, null, type, required, source, sourceKey, 0
        );
    }
}
