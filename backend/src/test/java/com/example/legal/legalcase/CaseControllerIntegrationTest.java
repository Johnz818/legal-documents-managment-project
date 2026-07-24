package com.example.legal.legalcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    CaseControllerIntegrationTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void hideExistingCasesForTestIsolation() {
        jdbcTemplate.update("DELETE FROM cases");
    }

    @Test
    void returnsEmptyDataWhenNoCasesExist() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsLatestTenNonArchivedCasesInDescendingOrder() throws Exception {
        String prefix = "API-" + UUID.randomUUID() + "-";
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        for (int index = 0; index < 12; index++) {
            insertCase(prefix + index, index, baseTime.plusMinutes(index), false);
        }
        insertCase(prefix + "ARCHIVED", 99, baseTime.plusDays(1), true);

        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].caseNumber").value(prefix + "11"))
                .andExpect(jsonPath("$.data[0].caseName").value("Test case 11"))
                .andExpect(jsonPath("$.data[0].status").value("审理中"))
                .andExpect(jsonPath("$.data[0].courtName").value("Test court"))
                .andExpect(jsonPath("$.data[0].caseCause").value("Test cause"))
                .andExpect(jsonPath("$.data[0].plaintiff").value("Test plaintiff"))
                .andExpect(jsonPath("$.data[0].leadLawyerName").value("Test lawyer"))
                .andExpect(jsonPath("$.data[0].filingDate").value("2026-01-10"))
                .andExpect(jsonPath("$.data[0].hearingDate").value("2026-02-20"))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].updatedAt").exists())
                .andExpect(jsonPath("$.data[0].archived").value(false))
                .andExpect(jsonPath("$.data[9].caseNumber").value(prefix + "2"));
    }

    @Test
    void returnsCompleteCaseDetail() throws Exception {
        String caseNumber = "DETAIL-" + UUID.randomUUID();
        Long caseId = insertCase(
                caseNumber,
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        jdbcTemplate.update(
                """
                UPDATE cases
                SET case_cause = ?,
                    filing_date = ?,
                    hearing_date = ?,
                    judgment_date = ?,
                    description = ?
                WHERE id = ?
                """,
                "Contract dispute",
                "2026-01-10",
                "2026-02-20",
                "2026-03-30",
                "Test description",
                caseId
        );

        mockMvc.perform(get("/api/cases/{id}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.caseNumber").value(caseNumber))
                .andExpect(jsonPath("$.caseName").value("Test case 1"))
                .andExpect(jsonPath("$.status").value("审理中"))
                .andExpect(jsonPath("$.courtName").value("Test court"))
                .andExpect(jsonPath("$.caseCause").value("Contract dispute"))
                .andExpect(jsonPath("$.plaintiff").value("Test plaintiff"))
                .andExpect(jsonPath("$.defendant").value("Test defendant"))
                .andExpect(jsonPath("$.leadLawyerName").value("Test lawyer"))
                .andExpect(jsonPath("$.filingDate").value("2026-01-10"))
                .andExpect(jsonPath("$.hearingDate").value("2026-02-20"))
                .andExpect(jsonPath("$.judgmentDate").value("2026-03-30"))
                .andExpect(jsonPath("$.description").value("Test description"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void returnsArchivedCaseDetail() throws Exception {
        Long caseId = insertCase(
                "ARCHIVED-DETAIL-" + UUID.randomUUID(),
                2,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                true
        );

        mockMvc.perform(get("/api/cases/{id}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void returnsNotFoundWhenCaseDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/cases/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    private Long insertCase(
            String caseNumber,
            int index,
            LocalDateTime timestamp,
            boolean archived
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO cases (
                    case_number,
                    case_name,
                    status,
                    court_name,
                    case_cause,
                    plaintiff,
                    defendant,
                    lead_lawyer_name,
                    filing_date,
                    hearing_date,
                    created_at,
                    updated_at,
                    archived
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                caseNumber,
                "Test case " + index,
                "IN_TRIAL",
                "Test court",
                "Test cause",
                "Test plaintiff",
                "Test defendant",
                "Test lawyer",
                "2026-01-10",
                "2026-02-20",
                timestamp,
                timestamp,
                archived
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM cases WHERE case_number = ?",
                Long.class,
                caseNumber
        );
    }
}
