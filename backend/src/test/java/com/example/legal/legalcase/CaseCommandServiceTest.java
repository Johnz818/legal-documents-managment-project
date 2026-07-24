package com.example.legal.legalcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseCommandServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Test
    void createsCaseAndNormalizesTextFields() {
        CaseCreateRequest request = new CaseCreateRequest(
                "  (2026)沪0115民初1001号  ",
                "  张三诉某公司劳动争议案  ",
                CaseStatus.IN_TRIAL,
                "  上海市浦东新区人民法院  ",
                "  劳动争议  ",
                "  张三  ",
                "  某公司  ",
                "  李律师  ",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 15),
                null,
                "  案件说明  "
        );
        when(caseRepository.existsByCaseNumber("(2026)沪0115民初1001号"))
                .thenReturn(false);
        when(caseRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(CaseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CaseCommandService service = new CaseCommandService(caseRepository);

        CaseDetailResponse response = service.createCase(request);

        ArgumentCaptor<CaseEntity> captor = ArgumentCaptor.forClass(CaseEntity.class);
        verify(caseRepository).saveAndFlush(captor.capture());
        CaseEntity savedCase = captor.getValue();
        assertThat(savedCase.getCaseNumber()).isEqualTo("(2026)沪0115民初1001号");
        assertThat(savedCase.getCaseName()).isEqualTo("张三诉某公司劳动争议案");
        assertThat(savedCase.getCourtName()).isEqualTo("上海市浦东新区人民法院");
        assertThat(savedCase.getCaseCause()).isEqualTo("劳动争议");
        assertThat(savedCase.getPlaintiff()).isEqualTo("张三");
        assertThat(savedCase.getDefendant()).isEqualTo("某公司");
        assertThat(savedCase.getLeadLawyerName()).isEqualTo("李律师");
        assertThat(savedCase.getFilingDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(savedCase.getHearingDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(savedCase.getJudgmentDate()).isNull();
        assertThat(savedCase.getDescription()).isEqualTo("案件说明");
        assertThat(savedCase.isArchived()).isFalse();
        assertThat(response.status()).isEqualTo("审理中");
    }

    @Test
    void convertsBlankOptionalTextToNull() {
        CaseCreateRequest request = requiredRequest("CASE-OPTIONAL-001");
        when(caseRepository.existsByCaseNumber("CASE-OPTIONAL-001")).thenReturn(false);
        when(caseRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(CaseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CaseCommandService service = new CaseCommandService(caseRepository);

        CaseDetailResponse response = service.createCase(request);

        assertThat(response.courtName()).isNull();
        assertThat(response.caseCause()).isNull();
        assertThat(response.description()).isNull();
    }

    @Test
    void rejectsDuplicateCaseNumberBeforeSaving() {
        CaseCreateRequest request = requiredRequest("CASE-DUPLICATE-001");
        when(caseRepository.existsByCaseNumber("CASE-DUPLICATE-001")).thenReturn(true);
        CaseCommandService service = new CaseCommandService(caseRepository);

        assertThatThrownBy(() -> service.createCase(request))
                .isInstanceOf(DuplicateCaseNumberException.class);
        verify(caseRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void translatesConcurrentUniqueConstraintFailure() {
        CaseCreateRequest request = requiredRequest("CASE-RACE-001");
        when(caseRepository.existsByCaseNumber("CASE-RACE-001")).thenReturn(false);
        when(caseRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(CaseEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        CaseCommandService service = new CaseCommandService(caseRepository);

        assertThatThrownBy(() -> service.createCase(request))
                .isInstanceOf(DuplicateCaseNumberException.class);
    }

    private CaseCreateRequest requiredRequest(String caseNumber) {
        return new CaseCreateRequest(
                caseNumber,
                "Test case",
                CaseStatus.PENDING_FILING,
                " ",
                null,
                "Plaintiff",
                "Defendant",
                "Lead lawyer",
                null,
                null,
                null,
                "\t"
        );
    }
}
