package com.example.legal.legalcase;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseCommandService {

    private final CaseRepository caseRepository;

    public CaseCommandService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @Transactional
    public CaseDetailResponse createCase(CaseCreateRequest request) {
        String caseNumber = request.caseNumber().trim();
        if (caseRepository.existsByCaseNumber(caseNumber)) {
            throw new DuplicateCaseNumberException(caseNumber);
        }

        CaseEntity caseEntity = new CaseEntity(
                caseNumber,
                request.caseName().trim(),
                request.status(),
                request.plaintiff().trim(),
                request.defendant().trim(),
                request.leadLawyerName().trim()
        );
        caseEntity.setCourtName(normalizeOptional(request.courtName()));
        caseEntity.setCaseCause(normalizeOptional(request.caseCause()));
        caseEntity.setFilingDate(request.filingDate());
        caseEntity.setHearingDate(request.hearingDate());
        caseEntity.setJudgmentDate(request.judgmentDate());
        caseEntity.setDescription(normalizeOptional(request.description()));

        try {
            return toResponse(caseRepository.saveAndFlush(caseEntity));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCaseNumberException(caseNumber);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private CaseDetailResponse toResponse(CaseEntity caseEntity) {
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
