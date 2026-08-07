package com.example.legal.document.generation;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentGenerationRepository extends Repository<DocumentGenerationEntity, Long> {

    DocumentGenerationEntity saveAndFlush(DocumentGenerationEntity generation);

    Optional<DocumentGenerationEntity> findById(Long id);

    Optional<DocumentGenerationEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select g.caseDocumentId as caseDocumentId, g.createdAt as createdAt
            from DocumentGenerationEntity g
            where g.caseId = :caseId and g.caseDocumentId in :caseDocumentIds
            """)
    List<CaseDocumentGenerationTimestamp> findCaseDocumentTimestamps(
            @Param("caseId") Long caseId,
            @Param("caseDocumentIds") Collection<Long> caseDocumentIds
    );
}
