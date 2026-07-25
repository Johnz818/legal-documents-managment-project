package com.example.legal.legalcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CaseUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String caseNumber,

        @NotBlank
        @Size(max = 255)
        String caseName,

        @NotNull
        CaseStatus status,

        @Size(max = 255)
        String courtName,

        @Size(max = 255)
        String caseCause,

        @NotBlank
        @Size(max = 255)
        String plaintiff,

        @NotBlank
        @Size(max = 255)
        String defendant,

        @NotBlank
        @Size(max = 255)
        String leadLawyerName,

        LocalDate filingDate,
        LocalDate hearingDate,
        LocalDate judgmentDate,
        String description,

        @NotNull
        @PositiveOrZero
        Long version
) {
}
