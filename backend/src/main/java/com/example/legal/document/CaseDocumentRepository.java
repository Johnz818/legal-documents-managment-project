package com.example.legal.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseDocumentRepository extends JpaRepository<CaseDocumentEntity, Long> {

    List<CaseDocumentEntity> findAllByCaseIdOrderByCreatedAtDescIdDesc(Long caseId);

    Optional<CaseDocumentEntity> findByIdAndCaseId(Long id, Long caseId);
}
