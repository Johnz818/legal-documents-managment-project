package com.example.legal.legalcase;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @Transactional
    public CaseDetailResponse updateCase(Long id, CaseUpdateRequest request) {
        CaseEntity caseEntity = caseRepository.findById(id)
                .orElseThrow(() -> new CaseNotFoundException(id));

        if (!request.version().equals(caseEntity.getVersion())) {
            throw new StaleCaseVersionException(id);
        }

        String caseNumber = request.caseNumber().trim();
        if (caseRepository.existsByCaseNumberAndIdNot(caseNumber, id)) {
            throw new DuplicateCaseNumberException(caseNumber);
        }

        caseEntity.setCaseNumber(caseNumber);
        caseEntity.setCaseName(request.caseName().trim());
        caseEntity.setStatus(request.status());
        caseEntity.setCourtName(normalizeOptional(request.courtName()));
        caseEntity.setCaseCause(normalizeOptional(request.caseCause()));
        caseEntity.setPlaintiff(request.plaintiff().trim());
        caseEntity.setDefendant(request.defendant().trim());
        caseEntity.setLeadLawyerName(request.leadLawyerName().trim());
        caseEntity.setFilingDate(request.filingDate());
        caseEntity.setHearingDate(request.hearingDate());
        caseEntity.setJudgmentDate(request.judgmentDate());
        caseEntity.setDescription(normalizeOptional(request.description()));

        try {
            return toResponse(caseRepository.saveAndFlush(caseEntity));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new StaleCaseVersionException(id);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCaseNumberException(caseNumber);
        }
    }

    @Transactional
    public CaseDetailResponse archiveCase(Long id, CaseArchiveRequest request) {
        return changeArchiveState(id, request.version(), true);
    }

    @Transactional
    public CaseDetailResponse restoreCase(Long id, CaseArchiveRequest request) {
        return changeArchiveState(id, request.version(), false);
    }

    private CaseDetailResponse changeArchiveState(Long id, Long version, boolean archived) {
        CaseEntity caseEntity = caseRepository.findById(id)
                .orElseThrow(() -> new CaseNotFoundException(id));

        if (!version.equals(caseEntity.getVersion())) {
            throw new StaleCaseVersionException(id);
        }

        if (caseEntity.isArchived() == archived) {
            return toResponse(caseEntity);
        }

        caseEntity.setArchived(archived);
        try {
            return toResponse(caseRepository.saveAndFlush(caseEntity));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new StaleCaseVersionException(id);
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
                caseEntity.isArchived(),
                caseEntity.getVersion()
        );
    }
}
