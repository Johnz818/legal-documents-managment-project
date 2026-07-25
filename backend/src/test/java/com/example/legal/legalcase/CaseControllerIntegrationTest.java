package com.example.legal.legalcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    CaseControllerIntegrationTest(
            MockMvc mockMvc,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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
    void filtersCasesByCombinedStructuredCriteria() throws Exception {
        String prefix = "(2016)浙01-" + UUID.randomUUID();
        Long matchingId = insertCase(
                prefix + "-MATCH",
                1,
                LocalDateTime.of(2026, 1, 2, 10, 0),
                false
        );
        Long wrongLawyerId = insertCase(
                prefix + "-WRONG-LAWYER",
                2,
                LocalDateTime.of(2026, 1, 3, 10, 0),
                false
        );
        Long archivedId = insertCase(
                prefix + "-ARCHIVED",
                3,
                LocalDateTime.of(2026, 1, 4, 10, 0),
                true
        );
        jdbcTemplate.update(
                """
                UPDATE cases
                SET case_name = ?, status = ?, lead_lawyer_name = ?
                WHERE id IN (?, ?)
                """,
                "张三合同纠纷",
                "IN_TRIAL",
                "李律师",
                matchingId,
                archivedId
        );
        jdbcTemplate.update(
                "UPDATE cases SET case_name = ?, lead_lawyer_name = ? WHERE id = ?",
                "张三合同纠纷",
                "王律师",
                wrongLawyerId
        );

        mockMvc.perform(get("/api/cases")
                        .param("caseNumberPrefix", prefix)
                        .param("caseNamePrefix", "张三")
                        .param("status", "IN_TRIAL")
                        .param("leadLawyerName", "李律师"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(matchingId));
    }

    @Test
    void treatsPrefixWildcardsAsLiteralCharacters() throws Exception {
        String prefix = "WILDCARD-" + UUID.randomUUID();
        insertCase(prefix + "%-MATCH", 1, LocalDateTime.of(2026, 1, 1, 10, 0), false);
        insertCase(prefix + "X-NO-MATCH", 2, LocalDateTime.of(2026, 1, 2, 10, 0), false);

        mockMvc.perform(get("/api/cases")
                        .param("caseNumberPrefix", prefix + "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].caseNumber").value(prefix + "%-MATCH"));
    }

    @Test
    void limitsFilteredCasesAndKeepsNewestFirst() throws Exception {
        String prefix = "FILTERED-" + UUID.randomUUID() + "-";
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        for (int index = 0; index < 12; index++) {
            insertCase(prefix + index, index, baseTime.plusMinutes(index), false);
        }

        mockMvc.perform(get("/api/cases")
                        .param("caseNumberPrefix", prefix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[0].caseNumber").value(prefix + "11"))
                .andExpect(jsonPath("$.data[9].caseNumber").value(prefix + "2"));
    }

    @Test
    void returnsEmptyDataWhenSearchCriteriaDoNotMatch() throws Exception {
        insertCase(
                "EXISTING-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );

        mockMvc.perform(get("/api/cases")
                        .param("caseNumberPrefix", "NONEXISTENT-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsBadRequestForInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/cases")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
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
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.version").value(0));
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

    @Test
    void createsCaseWithAllSupportedFields() throws Exception {
        String caseNumber = "CREATE-" + UUID.randomUUID();
        ObjectNode request = validCreateRequest(caseNumber);
        request.put("courtName", "上海市浦东新区人民法院");
        request.put("caseCause", "劳动争议");
        request.put("filingDate", "2026-07-01");
        request.put("hearingDate", "2026-08-15");
        request.put("judgmentDate", "2026-09-20");
        request.put("description", "案件说明");

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.matchesPattern(".*/api/cases/\\d+")
                ))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.caseNumber").value(caseNumber))
                .andExpect(jsonPath("$.caseName").value("张三诉某公司劳动争议案"))
                .andExpect(jsonPath("$.status").value("审理中"))
                .andExpect(jsonPath("$.courtName").value("上海市浦东新区人民法院"))
                .andExpect(jsonPath("$.caseCause").value("劳动争议"))
                .andExpect(jsonPath("$.plaintiff").value("张三"))
                .andExpect(jsonPath("$.defendant").value("某公司"))
                .andExpect(jsonPath("$.leadLawyerName").value("李律师"))
                .andExpect(jsonPath("$.filingDate").value("2026-07-01"))
                .andExpect(jsonPath("$.hearingDate").value("2026-08-15"))
                .andExpect(jsonPath("$.judgmentDate").value("2026-09-20"))
                .andExpect(jsonPath("$.description").value("案件说明"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.version").value(0));

        Integer persisted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cases WHERE case_number = ? AND archived = FALSE",
                Integer.class,
                caseNumber
        );
        org.assertj.core.api.Assertions.assertThat(persisted).isEqualTo(1);
    }

    @Test
    void createsCaseWithoutOptionalFields() throws Exception {
        String caseNumber = "CREATE-MINIMAL-" + UUID.randomUUID();

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validCreateRequest(caseNumber))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courtName").isEmpty())
                .andExpect(jsonPath("$.caseCause").isEmpty())
                .andExpect(jsonPath("$.filingDate").isEmpty())
                .andExpect(jsonPath("$.hearingDate").isEmpty())
                .andExpect(jsonPath("$.judgmentDate").isEmpty())
                .andExpect(jsonPath("$.description").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "caseNumber",
            "caseName",
            "status",
            "plaintiff",
            "defendant",
            "leadLawyerName"
    })
    void returnsBadRequestWhenRequiredFieldIsMissing(String fieldName) throws Exception {
        ObjectNode request = validCreateRequest("CREATE-MISSING-" + UUID.randomUUID());
        request.remove(fieldName);

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "caseNumber",
            "caseName",
            "plaintiff",
            "defendant",
            "leadLawyerName"
    })
    void returnsBadRequestWhenRequiredTextIsBlank(String fieldName) throws Exception {
        ObjectNode request = validCreateRequest("CREATE-BLANK-" + UUID.randomUUID());
        request.put(fieldName, " ");

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForInvalidCreateStatus() throws Exception {
        ObjectNode request = validCreateRequest("CREATE-STATUS-" + UUID.randomUUID());
        request.put("status", "UNKNOWN");

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForMalformedCreateDate() throws Exception {
        ObjectNode request = validCreateRequest("CREATE-DATE-" + UUID.randomUUID());
        request.put("filingDate", "07/24/2026");

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenCaseNumberExceedsColumnLength() throws Exception {
        ObjectNode request = validCreateRequest("C".repeat(101));

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsConflictForDuplicateCaseNumber() throws Exception {
        String caseNumber = "CREATE-DUPLICATE-" + UUID.randomUUID();
        insertCase(caseNumber, 1, LocalDateTime.of(2026, 1, 1, 10, 0), false);

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validCreateRequest(caseNumber))))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesAllEditableCaseFields() throws Exception {
        String originalCaseNumber = "UPDATE-ORIGINAL-" + UUID.randomUUID();
        Long caseId = insertCase(
                originalCaseNumber,
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        String updatedCaseNumber = "UPDATE-CHANGED-" + UUID.randomUUID();
        ObjectNode request = validUpdateRequest(updatedCaseNumber, 0);
        request.put("courtName", "更新后的法院");
        request.put("caseCause", "更新后的案由");
        request.put("filingDate", "2026-07-01");
        request.put("hearingDate", "2026-08-15");
        request.put("judgmentDate", "2026-09-20");
        request.put("description", "更新后的说明");

        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.caseNumber").value(updatedCaseNumber))
                .andExpect(jsonPath("$.caseName").value("更新后的案件"))
                .andExpect(jsonPath("$.status").value("已判决(生效)"))
                .andExpect(jsonPath("$.courtName").value("更新后的法院"))
                .andExpect(jsonPath("$.caseCause").value("更新后的案由"))
                .andExpect(jsonPath("$.plaintiff").value("更新后的原告"))
                .andExpect(jsonPath("$.defendant").value("更新后的被告"))
                .andExpect(jsonPath("$.leadLawyerName").value("更新后的律师"))
                .andExpect(jsonPath("$.filingDate").value("2026-07-01"))
                .andExpect(jsonPath("$.hearingDate").value("2026-08-15"))
                .andExpect(jsonPath("$.judgmentDate").value("2026-09-20"))
                .andExpect(jsonPath("$.description").value("更新后的说明"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.version").value(1));

        String persistedCaseName = jdbcTemplate.queryForObject(
                "SELECT case_name FROM cases WHERE id = ?",
                String.class,
                caseId
        );
        org.assertj.core.api.Assertions.assertThat(persistedCaseName).isEqualTo("更新后的案件");
        Long persistedVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM cases WHERE id = ?",
                Long.class,
                caseId
        );
        org.assertj.core.api.Assertions.assertThat(persistedVersion).isEqualTo(1L);
        LocalDateTime persistedUpdatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM cases WHERE id = ?",
                LocalDateTime.class,
                caseId
        );
        org.assertj.core.api.Assertions.assertThat(persistedUpdatedAt)
                .isAfter(LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    void clearsOptionalFieldsDuringUpdate() throws Exception {
        Long caseId = insertCase(
                "UPDATE-CLEAR-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        ObjectNode request = validUpdateRequest("UPDATE-CLEAR-RESULT-" + UUID.randomUUID(), 0);

        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courtName").isEmpty())
                .andExpect(jsonPath("$.caseCause").isEmpty())
                .andExpect(jsonPath("$.filingDate").isEmpty())
                .andExpect(jsonPath("$.hearingDate").isEmpty())
                .andExpect(jsonPath("$.judgmentDate").isEmpty())
                .andExpect(jsonPath("$.description").isEmpty());
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingCase() throws Exception {
        mockMvc.perform(put("/api/cases/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                validUpdateRequest("UPDATE-MISSING-" + UUID.randomUUID(), 0)
                        )))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictWhenUpdateUsesDuplicateCaseNumber() throws Exception {
        String existingCaseNumber = "UPDATE-DUPLICATE-" + UUID.randomUUID();
        insertCase(existingCaseNumber, 1, LocalDateTime.of(2026, 1, 1, 10, 0), false);
        Long caseId = insertCase(
                "UPDATE-TARGET-" + UUID.randomUUID(),
                2,
                LocalDateTime.of(2026, 1, 2, 10, 0),
                false
        );

        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                validUpdateRequest(existingCaseNumber, 0)
                        )))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsConflictWhenUpdateVersionIsStale() throws Exception {
        Long caseId = insertCase(
                "UPDATE-STALE-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        jdbcTemplate.update("UPDATE cases SET version = 2 WHERE id = ?", caseId);

        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                validUpdateRequest("UPDATE-STALE-RESULT-" + UUID.randomUUID(), 1)
                        )))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "caseNumber",
            "caseName",
            "status",
            "plaintiff",
            "defendant",
            "leadLawyerName",
            "version"
    })
    void returnsBadRequestWhenRequiredUpdateFieldIsMissing(String fieldName) throws Exception {
        Long caseId = insertCase(
                "UPDATE-INVALID-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        ObjectNode request = validUpdateRequest("UPDATE-VALID-" + UUID.randomUUID(), 0);
        request.remove(fieldName);

        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForInvalidUpdateStatus() throws Exception {
        Long caseId = insertCase(
                "UPDATE-STATUS-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        ObjectNode request = validUpdateRequest("UPDATE-STATUS-RESULT-" + UUID.randomUUID(), 0);
        request.put("status", "UNKNOWN");

        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForMalformedUpdateDate() throws Exception {
        Long caseId = insertCase(
                "UPDATE-DATE-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        ObjectNode request = validUpdateRequest("UPDATE-DATE-RESULT-" + UUID.randomUUID(), 0);
        request.put("hearingDate", "08/15/2026");

        mockMvc.perform(put("/api/cases/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archivesAndRestoresCase() throws Exception {
        Long caseId = insertCase(
                "ARCHIVE-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );

        mockMvc.perform(post("/api/cases/{id}/archive", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/cases/{id}/restore", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void archiveAndRestoreAreIdempotentForCurrentVersion() throws Exception {
        Long activeCaseId = insertCase(
                "RESTORE-IDEMPOTENT-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        Long archivedCaseId = insertCase(
                "ARCHIVE-IDEMPOTENT-" + UUID.randomUUID(),
                2,
                LocalDateTime.of(2026, 1, 2, 10, 0),
                true
        );

        mockMvc.perform(post("/api/cases/{id}/restore", activeCaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(post("/api/cases/{id}/archive", archivedCaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void returnsArchivedCasesOnlyWhenRequested() throws Exception {
        insertCase(
                "ACTIVE-LIST-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        String archivedCaseNumber = "ARCHIVED-LIST-" + UUID.randomUUID();
        insertCase(
                archivedCaseNumber,
                2,
                LocalDateTime.of(2026, 1, 2, 10, 0),
                true
        );

        mockMvc.perform(get("/api/cases").param("archiveState", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].caseNumber").value(archivedCaseNumber))
                .andExpect(jsonPath("$.data[0].archived").value(true));
    }

    @Test
    void appliesSearchCriteriaToArchivedCases() throws Exception {
        String prefix = "ARCHIVED-SEARCH-" + UUID.randomUUID();
        insertCase(
                prefix + "-MATCH",
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                true
        );
        insertCase(
                "OTHER-" + UUID.randomUUID(),
                2,
                LocalDateTime.of(2026, 1, 2, 10, 0),
                true
        );

        mockMvc.perform(get("/api/cases")
                        .param("archiveState", "ARCHIVED")
                        .param("caseNumberPrefix", prefix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].caseNumber").value(prefix + "-MATCH"));
    }

    @Test
    void returnsConflictForStaleArchiveVersion() throws Exception {
        Long caseId = insertCase(
                "ARCHIVE-STALE-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        jdbcTemplate.update("UPDATE cases SET version = 2 WHERE id = ?", caseId);

        mockMvc.perform(post("/api/cases/{id}/archive", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundAndBadRequestForInvalidArchiveRequests() throws Exception {
        mockMvc.perform(post("/api/cases/{id}/archive", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isNotFound());

        Long caseId = insertCase(
                "ARCHIVE-INVALID-" + UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                false
        );
        mockMvc.perform(post("/api/cases/{id}/archive", caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private ObjectNode validCreateRequest(String caseNumber) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("caseNumber", caseNumber);
        request.put("caseName", "张三诉某公司劳动争议案");
        request.put("status", "IN_TRIAL");
        request.put("plaintiff", "张三");
        request.put("defendant", "某公司");
        request.put("leadLawyerName", "李律师");
        return request;
    }

    private ObjectNode validUpdateRequest(String caseNumber, long version) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("caseNumber", caseNumber);
        request.put("caseName", "更新后的案件");
        request.put("status", "FINAL_JUDGMENT");
        request.put("plaintiff", "更新后的原告");
        request.put("defendant", "更新后的被告");
        request.put("leadLawyerName", "更新后的律师");
        request.put("version", version);
        return request;
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
