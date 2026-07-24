package com.example.legal.legalcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseQueryServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseEntity caseEntity;

    @Test
    void returnsEmptyDataWhenNoCasesExist() {
        when(caseRepository.findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of());
        CaseQueryService service = new CaseQueryService(caseRepository);

        CaseListResponse response = service.getLatestCases();

        assertThat(response.data()).isEmpty();
        verify(caseRepository).findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc();
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_FILING,待立案",
            "PRE_TRIAL_PREPARATION,审理准备",
            "IN_TRIAL,审理中",
            "CLOSED,已结案"
    })
    void convertsStatusToChineseDisplayName(CaseStatus status, String displayName) {
        when(caseEntity.getStatus()).thenReturn(status);
        when(caseRepository.findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(caseEntity));
        CaseQueryService service = new CaseQueryService(caseRepository);

        CaseListResponse response = service.getLatestCases();

        assertThat(response.data()).singleElement()
                .extracting(CaseSummaryResponse::status)
                .isEqualTo(displayName);
    }

    @Test
    void returnsCaseListDisplayFields() {
        LocalDate filingDate = LocalDate.of(2026, 1, 10);
        LocalDate hearingDate = LocalDate.of(2026, 2, 20);
        when(caseEntity.getStatus()).thenReturn(CaseStatus.IN_TRIAL);
        when(caseEntity.getCaseCause()).thenReturn("Contract dispute");
        when(caseEntity.getPlaintiff()).thenReturn("Test plaintiff");
        when(caseEntity.getFilingDate()).thenReturn(filingDate);
        when(caseEntity.getHearingDate()).thenReturn(hearingDate);
        when(caseRepository.findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(caseEntity));
        CaseQueryService service = new CaseQueryService(caseRepository);

        CaseSummaryResponse response = service.getLatestCases().data().getFirst();

        assertThat(response.caseCause()).isEqualTo("Contract dispute");
        assertThat(response.plaintiff()).isEqualTo("Test plaintiff");
        assertThat(response.filingDate()).isEqualTo(filingDate);
        assertThat(response.hearingDate()).isEqualTo(hearingDate);
    }

    @Test
    void returnsCompleteCaseDetail() {
        Long caseId = 42L;
        LocalDate filingDate = LocalDate.of(2026, 1, 10);
        LocalDate hearingDate = LocalDate.of(2026, 2, 20);
        LocalDate judgmentDate = LocalDate.of(2026, 3, 30);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 11, 0);
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));
        when(caseEntity.getId()).thenReturn(caseId);
        when(caseEntity.getCaseNumber()).thenReturn("CASE-DETAIL-001");
        when(caseEntity.getCaseName()).thenReturn("Test detail case");
        when(caseEntity.getStatus()).thenReturn(CaseStatus.IN_TRIAL);
        when(caseEntity.getCourtName()).thenReturn("Test court");
        when(caseEntity.getCaseCause()).thenReturn("Contract dispute");
        when(caseEntity.getPlaintiff()).thenReturn("Test plaintiff");
        when(caseEntity.getDefendant()).thenReturn("Test defendant");
        when(caseEntity.getLeadLawyerName()).thenReturn("Test lawyer");
        when(caseEntity.getFilingDate()).thenReturn(filingDate);
        when(caseEntity.getHearingDate()).thenReturn(hearingDate);
        when(caseEntity.getJudgmentDate()).thenReturn(judgmentDate);
        when(caseEntity.getDescription()).thenReturn("Test description");
        when(caseEntity.getCreatedAt()).thenReturn(createdAt);
        when(caseEntity.getUpdatedAt()).thenReturn(updatedAt);
        when(caseEntity.isArchived()).thenReturn(true);
        CaseQueryService service = new CaseQueryService(caseRepository);

        Optional<CaseDetailResponse> response = service.getCaseById(caseId);

        assertThat(response).contains(new CaseDetailResponse(
                caseId,
                "CASE-DETAIL-001",
                "Test detail case",
                "审理中",
                "Test court",
                "Contract dispute",
                "Test plaintiff",
                "Test defendant",
                "Test lawyer",
                filingDate,
                hearingDate,
                judgmentDate,
                "Test description",
                createdAt,
                updatedAt,
                true
        ));
        verify(caseRepository).findById(caseId);
    }

    @Test
    void returnsEmptyWhenCaseDoesNotExist() {
        Long caseId = 404L;
        when(caseRepository.findById(caseId)).thenReturn(Optional.empty());
        CaseQueryService service = new CaseQueryService(caseRepository);

        Optional<CaseDetailResponse> response = service.getCaseById(caseId);

        assertThat(response).isEmpty();
        verify(caseRepository).findById(caseId);
    }
}
