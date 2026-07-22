package com.example.legal.legalcase;

import java.time.LocalDateTime;

public record CaseSummaryResponse(
        Long id,
        String caseNumber,
        String caseName,
        String status,
        String courtName,
        String leadLawyerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean archived
) {
}
