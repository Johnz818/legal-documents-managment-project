package com.example.legal.legalcase;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CaseRepositoryIntegrationTest {

    private final CaseRepository caseRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    CaseRepositoryIntegrationTest(
            CaseRepository caseRepository,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate
    ) {
        this.caseRepository = caseRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void persistsAndRetrievesCaseEntity() {
        String caseNumber = "TEST-" + UUID.randomUUID();
        CaseEntity caseEntity = new CaseEntity(
                caseNumber,
                "Test case",
                CaseStatus.PRE_TRIAL_PREPARATION,
                "Test plaintiff",
                "Test defendant",
                "Test lawyer"
        );
        caseEntity.setCourtName("Test court");
        caseEntity.setCaseCause("Contract dispute");
        caseEntity.setFilingDate(LocalDate.of(2026, 1, 10));
        caseEntity.setHearingDate(LocalDate.of(2026, 2, 20));
        caseEntity.setJudgmentDate(LocalDate.of(2026, 3, 30));
        caseEntity.setDescription("Test case description");

        CaseEntity saved = caseRepository.saveAndFlush(caseEntity);
        Long savedId = saved.getId();
        entityManager.clear();

        CaseEntity retrieved = caseRepository.findById(savedId).orElseThrow();

        assertThat(retrieved.getCaseNumber()).isEqualTo(caseNumber);
        assertThat(retrieved.getCaseName()).isEqualTo("Test case");
        assertThat(retrieved.getStatus()).isEqualTo(CaseStatus.PRE_TRIAL_PREPARATION);
        assertThat(retrieved.getCourtName()).isEqualTo("Test court");
        assertThat(retrieved.getCaseCause()).isEqualTo("Contract dispute");
        assertThat(retrieved.getPlaintiff()).isEqualTo("Test plaintiff");
        assertThat(retrieved.getDefendant()).isEqualTo("Test defendant");
        assertThat(retrieved.getLeadLawyerName()).isEqualTo("Test lawyer");
        assertThat(retrieved.getFilingDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(retrieved.getHearingDate()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(retrieved.getJudgmentDate()).isEqualTo(LocalDate.of(2026, 3, 30));
        assertThat(retrieved.getDescription()).isEqualTo("Test case description");
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();
        assertThat(retrieved.isArchived()).isFalse();

        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM cases WHERE id = ?",
                String.class,
                savedId
        );
        assertThat(storedStatus).isEqualTo("PRE_TRIAL_PREPARATION");
    }

    @Test
    void allowsOptionalCoreFieldsToBeNull() {
        CaseEntity caseEntity = new CaseEntity(
                uniqueCaseNumber(),
                "Incomplete test case",
                CaseStatus.PENDING_FILING,
                "Test plaintiff",
                "Test defendant",
                "Test lawyer"
        );

        CaseEntity saved = caseRepository.saveAndFlush(caseEntity);
        entityManager.clear();

        CaseEntity retrieved = caseRepository.findById(saved.getId()).orElseThrow();

        assertThat(retrieved.getCourtName()).isNull();
        assertThat(retrieved.getCaseCause()).isNull();
        assertThat(retrieved.getFilingDate()).isNull();
        assertThat(retrieved.getHearingDate()).isNull();
        assertThat(retrieved.getJudgmentDate()).isNull();
        assertThat(retrieved.getDescription()).isNull();
    }

    @Test
    void rejectsDuplicateCaseNumber() {
        String caseNumber = uniqueCaseNumber();
        caseRepository.saveAndFlush(new CaseEntity(
                caseNumber,
                "First test case",
                CaseStatus.PENDING_FILING,
                "First plaintiff",
                "First defendant",
                "First lawyer"
        ));

        CaseEntity duplicate = new CaseEntity(
                caseNumber,
                "Duplicate test case",
                CaseStatus.IN_TRIAL,
                "Second plaintiff",
                "Second defendant",
                "Second lawyer"
        );

        assertThatThrownBy(() -> caseRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullCaseName() {
        assertThatThrownBy(() -> insertCase(uniqueCaseNumber(), null, "PENDING_FILING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullStatus() {
        assertThatThrownBy(() -> insertCase(uniqueCaseNumber(), "Test case", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullPlaintiff() {
        assertThatThrownBy(() -> insertCaseWithParticipants(
                uniqueCaseNumber(),
                null,
                "Test defendant",
                "Test lawyer"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullDefendant() {
        assertThatThrownBy(() -> insertCaseWithParticipants(
                uniqueCaseNumber(),
                "Test plaintiff",
                null,
                "Test lawyer"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNullLeadLawyer() {
        assertThatThrownBy(() -> insertCaseWithParticipants(
                uniqueCaseNumber(),
                "Test plaintiff",
                "Test defendant",
                null
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void defaultsArchivedToFalseWhenNotProvided() {
        String caseNumber = uniqueCaseNumber();
        insertCase(caseNumber, "Test case", "PENDING_FILING");

        Boolean archived = jdbcTemplate.queryForObject(
                "SELECT archived FROM cases WHERE case_number = ?",
                Boolean.class,
                caseNumber
        );

        assertThat(archived).isFalse();
    }

    @Test
    void rejectsInvalidStatus() {
        assertThatThrownBy(() -> insertCase(uniqueCaseNumber(), "Test case", "INVALID_STATUS"))
                .isInstanceOf(DataAccessException.class);
    }

    private void insertCase(String caseNumber, String caseName, String status) {
        jdbcTemplate.update(
                """
                INSERT INTO cases (
                    case_number,
                    case_name,
                    status,
                    plaintiff,
                    defendant,
                    lead_lawyer_name,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
                caseNumber,
                caseName,
                status,
                "Test plaintiff",
                "Test defendant",
                "Test lawyer"
        );
    }

    private void insertCaseWithParticipants(
            String caseNumber,
            String plaintiff,
            String defendant,
            String leadLawyerName
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO cases (
                    case_number,
                    case_name,
                    status,
                    plaintiff,
                    defendant,
                    lead_lawyer_name,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
                caseNumber,
                "Test case",
                "PENDING_FILING",
                plaintiff,
                defendant,
                leadLawyerName
        );
    }

    private String uniqueCaseNumber() {
        return "TEST-" + UUID.randomUUID();
    }
}
