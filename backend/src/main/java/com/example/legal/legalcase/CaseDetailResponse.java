package com.example.legal.legalcase;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CaseDetailResponse(
        Long id,
        String caseNumber,
        String caseName,
        String status,
        String courtName,
        String caseCause,
        String plaintiff,
        String defendant,
        String leadLawyerName,
        LocalDate filingDate,
        LocalDate hearingDate,
        LocalDate judgmentDate,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean archived,
        Long version
) {
}
