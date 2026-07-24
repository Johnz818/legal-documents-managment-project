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
    public CaseListResponse getLatestCases() {
        return new CaseListResponse(
                caseRepository.findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
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
                caseEntity.getLeadLawyerName(),
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
                caseEntity.isArchived()
        );
    }
}
