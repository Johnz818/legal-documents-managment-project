package com.example.legal.legalcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
}
