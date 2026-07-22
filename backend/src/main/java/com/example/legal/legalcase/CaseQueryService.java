package com.example.legal.legalcase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
