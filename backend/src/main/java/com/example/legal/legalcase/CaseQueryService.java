package com.example.legal.legalcase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CaseQueryService {

    private final CaseRepository caseRepository;

    public CaseQueryService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @Transactional(readOnly = true)
    public CaseListResponse getCases(
            String caseNumberPrefix,
            String caseNamePrefix,
            CaseStatus status,
            String leadLawyerName
    ) {
        String normalizedCaseNumberPrefix = normalizePrefix(caseNumberPrefix);
        String normalizedCaseNamePrefix = normalizePrefix(caseNamePrefix);
        String normalizedLeadLawyerName = normalizeExact(leadLawyerName);

        if (normalizedCaseNumberPrefix == null
                && normalizedCaseNamePrefix == null
                && status == null
                && normalizedLeadLawyerName == null) {
            return getLatestCases();
        }

        return new CaseListResponse(
                caseRepository.searchTop10(
                                normalizedCaseNumberPrefix,
                                normalizedCaseNamePrefix,
                                status == null ? null : status.name(),
                                normalizedLeadLawyerName
                        )
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public CaseListResponse getLatestCases() {
        return new CaseListResponse(
                caseRepository.findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private String normalizePrefix(String value) {
        String normalized = normalizeExact(value);
        if (normalized == null) {
            return null;
        }

        return normalized
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private String normalizeExact(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Transactional(readOnly = true)
    public Optional<CaseDetailResponse> getCaseById(Long id) {
        return caseRepository.findById(id)
                .map(this::toDetailResponse);
    }

    private CaseSummaryResponse toResponse(CaseEntity caseEntity) {
        return new CaseSummaryResponse(
                caseEntity.getId(),
                caseEntity.getCaseNumber(),
                caseEntity.getCaseName(),
                caseEntity.getStatus().getDisplayName(),
                caseEntity.getCourtName(),
                caseEntity.getCaseCause(),
                caseEntity.getPlaintiff(),
                caseEntity.getLeadLawyerName(),
                caseEntity.getFilingDate(),
                caseEntity.getHearingDate(),
                caseEntity.getCreatedAt(),
                caseEntity.getUpdatedAt(),
                caseEntity.isArchived()
        );
    }

    private CaseDetailResponse toDetailResponse(CaseEntity caseEntity) {
        return new CaseDetailResponse(
                caseEntity.getId(),
                caseEntity.getCaseNumber(),
                caseEntity.getCaseName(),
                caseEntity.getStatus().getDisplayName(),
                caseEntity.getCourtName(),
                caseEntity.getCaseCause(),
                caseEntity.getPlaintiff(),
                caseEntity.getDefendant(),
                caseEntity.getLeadLawyerName(),
                caseEntity.getFilingDate(),
                caseEntity.getHearingDate(),
                caseEntity.getJudgmentDate(),
                caseEntity.getDescription(),
                caseEntity.getCreatedAt(),
                caseEntity.getUpdatedAt(),
                caseEntity.isArchived(),
                caseEntity.getVersion()
        );
    }
}
