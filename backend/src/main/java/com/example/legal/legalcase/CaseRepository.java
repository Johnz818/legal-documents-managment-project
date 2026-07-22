package com.example.legal.legalcase;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseRepository extends JpaRepository<CaseEntity, Long> {

    List<CaseEntity> findTop10ByArchivedFalseOrderByCreatedAtDescIdDesc();
}
