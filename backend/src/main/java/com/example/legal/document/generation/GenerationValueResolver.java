package com.example.legal.document.generation;

import com.example.legal.document.template.DocumentFieldDefaultSource;
import com.example.legal.document.template.DocumentTemplateFieldEntity;
import com.example.legal.legalcase.CaseEntity;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class GenerationValueResolver {

    private static final DateTimeFormatter CHINESE_DATE = DateTimeFormatter.ofPattern("uuuu年MM月dd日");

    private final Clock clock;

    public GenerationValueResolver(Clock clock) {
        this.clock = clock;
    }

    public String resolve(DocumentTemplateFieldEntity field, CaseEntity legalCase, ZoneId timezone) {
        if (field.getDefaultSource() == DocumentFieldDefaultSource.USER_INPUT) {
            return null;
        }
        if (field.getDefaultSource() == DocumentFieldDefaultSource.SYSTEM_VALUE) {
            return "currentDate".equals(field.getSourceKey())
                    ? LocalDate.now(clock.withZone(timezone)).format(CHINESE_DATE)
                    : null;
        }
        return resolveCaseValue(field.getSourceKey(), legalCase);
    }

    private String resolveCaseValue(String sourceKey, CaseEntity legalCase) {
        return switch (sourceKey) {
            case "caseNumber" -> legalCase.getCaseNumber();
            case "caseName" -> legalCase.getCaseName();
            case "courtName" -> legalCase.getCourtName();
            case "caseCause" -> legalCase.getCaseCause();
            case "plaintiff" -> legalCase.getPlaintiff();
            case "defendant" -> legalCase.getDefendant();
            case "leadLawyerName" -> legalCase.getLeadLawyerName();
            case "description" -> legalCase.getDescription();
            case "filingDate" -> format(legalCase.getFilingDate());
            case "hearingDate" -> format(legalCase.getHearingDate());
            case "judgmentDate" -> format(legalCase.getJudgmentDate());
            default -> null;
        };
    }

    private String format(LocalDate date) {
        return date == null ? null : date.format(CHINESE_DATE);
    }
}
