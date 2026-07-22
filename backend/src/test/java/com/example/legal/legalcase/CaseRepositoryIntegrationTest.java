package com.example.legal.legalcase;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
