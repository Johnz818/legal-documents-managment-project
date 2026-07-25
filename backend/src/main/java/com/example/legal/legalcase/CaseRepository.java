package com.example.legal.legalcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CaseRepository extends JpaRepository<CaseEntity, Long> {

    boolean existsByCaseNumber(String caseNumber);

    boolean existsByCaseNumberAndIdNot(String caseNumber, Long id);

    List<CaseEntity> findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc();

    List<CaseEntity> findTop10ByArchivedTrueOrderByCreatedAtDescIdDesc();

    @Query(value = """
            SELECT *
            FROM cases c
            WHERE c.archived = :archived
              AND (:caseNumberPrefix IS NULL
                   OR c.case_number LIKE CONCAT(:caseNumberPrefix, '%') ESCAPE '\\\\')
              AND (:caseNamePrefix IS NULL
                   OR c.case_name LIKE CONCAT(:caseNamePrefix, '%') ESCAPE '\\\\')
              AND (:status IS NULL OR c.status = :status)
              AND (:leadLawyerName IS NULL OR c.lead_lawyer_name = :leadLawyerName)
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT 10
            """, nativeQuery = true)
    List<CaseEntity> searchTop10(
            @Param("caseNumberPrefix") String caseNumberPrefix,
            @Param("caseNamePrefix") String caseNamePrefix,
            @Param("status") String status,
            @Param("leadLawyerName") String leadLawyerName,
            @Param("archived") boolean archived
    );
}
