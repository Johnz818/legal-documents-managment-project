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
                .andExpect(jsonPath("$.data[0].leadLawyerName").value("Test lawyer"))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].updatedAt").exists())
                .andExpect(jsonPath("$.data[0].archived").value(false))
                .andExpect(jsonPath("$.data[9].caseNumber").value(prefix + "2"));
    }

    private void insertCase(
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
                    plaintiff,
                    defendant,
                    lead_lawyer_name,
                    created_at,
                    updated_at,
                    archived
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                caseNumber,
                "Test case " + index,
                "IN_TRIAL",
                "Test court",
                "Test plaintiff",
                "Test defendant",
                "Test lawyer",
                timestamp,
                timestamp,
                archived
        );
    }
}
