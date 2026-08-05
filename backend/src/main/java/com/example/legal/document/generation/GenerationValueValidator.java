package com.example.legal.document.generation;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentFieldValueType;
import com.example.legal.document.template.DocumentTemplateFieldEntity;
import com.example.legal.legalcase.CaseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GenerationValueValidator {

    static final int MAX_VALUE_CODE_POINTS = 10_000;
    static final int MAX_AGGREGATE_CODE_POINTS = 100_000;
    private static final Pattern DECIMAL = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?");
    private static final Pattern CHINESE_DATE = Pattern.compile("([0-9]{4})年([0-9]{1,2})月([0-9]{1,2})日");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final GenerationValueResolver resolver;

    public GenerationValueValidator(GenerationValueResolver resolver) {
        this.resolver = resolver;
    }

    public List<ValidatedGenerationValue> validate(
            List<DocumentTemplateFieldEntity> fields,
            List<ReviewedGenerationValue> submitted,
            CaseEntity legalCase,
            ZoneId timezone
    ) {
        List<ValidatedGenerationValue> values = validateRequest(fields, submitted);
        validateDeterministicSources(values, legalCase, timezone);
        return values;
    }

    public List<ValidatedGenerationValue> validateRequest(
            List<DocumentTemplateFieldEntity> fields,
            List<ReviewedGenerationValue> submitted
    ) {
        Map<String, DocumentTemplateFieldEntity> contract = new HashMap<>();
        fields.forEach(field -> contract.put(field.getFieldKey(), field));
        Set<String> seen = new HashSet<>();
        List<ValidatedGenerationValue> result = new ArrayList<>(submitted.size());
        int aggregateCodePoints = 0;

        for (ReviewedGenerationValue value : submitted) {
            DocumentTemplateFieldEntity field = contract.get(value.fieldKey());
            if (field == null || !seen.add(value.fieldKey())) {
                throw invalid("Generation values must contain each template field exactly once");
            }
            validateLexical(field, value.value());
            aggregateCodePoints += value.value().codePointCount(0, value.value().length());
            if (aggregateCodePoints > MAX_AGGREGATE_CODE_POINTS) {
                throw invalid("Generation values exceed the aggregate size limit");
            }
            result.add(new ValidatedGenerationValue(field, value.value(), value.valueSource()));
        }
        if (seen.size() != contract.size()) {
            throw invalid("Generation values must contain each template field exactly once");
        }
        return result;
    }

    public void validateDeterministicSources(
            List<ValidatedGenerationValue> values,
            CaseEntity legalCase,
            ZoneId timezone
    ) {
        values.forEach(value -> validateSource(
                value.field(),
                new ReviewedGenerationValue(value.field().getFieldKey(), value.value(), value.valueSource()),
                legalCase,
                timezone
        ));
    }

    public boolean isUsableSuggestion(DocumentTemplateFieldEntity field, String value) {
        if (value == null) {
            return false;
        }
        try {
            validateLexical(field, value);
            return true;
        } catch (DocumentGenerationException exception) {
            return false;
        }
    }

    private void validateLexical(DocumentTemplateFieldEntity field, String value) {
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints > MAX_VALUE_CODE_POINTS || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw invalid("Generation values must be bounded one-line scalar strings");
        }
        if (field.isRequired() && value.isBlank()) {
            throw invalid("Required generation values must not be blank");
        }
        if (value.isEmpty() && !field.isRequired()) {
            return;
        }
        boolean valid = switch (field.getValueType()) {
            case TEXT -> true;
            case DECIMAL -> DECIMAL.matcher(value).matches();
            case BOOLEAN -> value.equals("true") || value.equals("false");
            case DATE -> parseDate(value) != null;
        };
        if (!valid) {
            throw invalid("Generation value does not match its declared scalar type");
        }
    }

    private void validateSource(
            DocumentTemplateFieldEntity field,
            ReviewedGenerationValue value,
            CaseEntity legalCase,
            ZoneId timezone
    ) {
        if (value.valueSource() == GenerationValueSource.USER_INPUT) {
            return;
        }
        DocumentFieldDefaultSource expectedSource = DocumentFieldDefaultSource.valueOf(value.valueSource().name());
        String current = resolver.resolve(field, legalCase, timezone);
        if (field.getDefaultSource() != expectedSource || current == null || !semanticallyEqual(
                field.getValueType(), current, value.value()
        )) {
            throw new DocumentGenerationException(
                    GenerationErrorCode.GENERATION_VALUE_STALE,
                    HttpStatus.CONFLICT,
                    "A reviewed deterministic value is no longer current",
                    Map.of("fieldKey", field.getFieldKey())
            );
        }
    }

    private boolean semanticallyEqual(DocumentFieldValueType type, String left, String right) {
        if (type == DocumentFieldValueType.DATE) {
            return parseDate(left).equals(parseDate(right));
        }
        return left.equals(right);
    }

    private LocalDate parseDate(String value) {
        try {
            if (value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                return LocalDate.parse(value, ISO_DATE);
            }
            Matcher matcher = CHINESE_DATE.matcher(value);
            if (matcher.matches()) {
                return LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
            }
            return null;
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private DocumentGenerationException invalid(String message) {
        return new DocumentGenerationException(
                GenerationErrorCode.GENERATION_REQUEST_INVALID,
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    public record ValidatedGenerationValue(
            DocumentTemplateFieldEntity field,
            String value,
            GenerationValueSource valueSource
    ) {
    }
}
