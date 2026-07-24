package com.example.legal.legalcase;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CaseSummaryResponse(
        Long id,
        String caseNumber,
        String caseName,
        String status,
        String courtName,
        String caseCause,
        String plaintiff,
        String leadLawyerName,
        LocalDate filingDate,
        LocalDate hearingDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean archived
) {
}
