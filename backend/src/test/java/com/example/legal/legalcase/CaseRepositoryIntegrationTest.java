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
                CaseStatus.PRE_TRIAL_PREPARATION
        );
        caseEntity.setCourtName("Test court");
        caseEntity.setLeadLawyerName("Test lawyer");

        CaseEntity saved = caseRepository.saveAndFlush(caseEntity);
        Long savedId = saved.getId();
        entityManager.clear();

        CaseEntity retrieved = caseRepository.findById(savedId).orElseThrow();

        assertThat(retrieved.getCaseNumber()).isEqualTo(caseNumber);
        assertThat(retrieved.getCaseName()).isEqualTo("Test case");
        assertThat(retrieved.getStatus()).isEqualTo(CaseStatus.PRE_TRIAL_PREPARATION);
        assertThat(retrieved.getCourtName()).isEqualTo("Test court");
        assertThat(retrieved.getLeadLawyerName()).isEqualTo("Test lawyer");
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
    void rejectsDuplicateCaseNumber() {
        String caseNumber = uniqueCaseNumber();
        caseRepository.saveAndFlush(new CaseEntity(
                caseNumber,
                "First test case",
                CaseStatus.PENDING_FILING
        ));

        CaseEntity duplicate = new CaseEntity(
                caseNumber,
                "Duplicate test case",
                CaseStatus.IN_TRIAL
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
                INSERT INTO cases (case_number, case_name, status, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
                caseNumber,
                caseName,
                status
        );
    }

    private String uniqueCaseNumber() {
        return "TEST-" + UUID.randomUUID();
    }
}
