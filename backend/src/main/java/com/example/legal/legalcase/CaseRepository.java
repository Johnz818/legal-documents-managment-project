package com.example.legal.legalcase;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<CaseEntity, Long> {
}
