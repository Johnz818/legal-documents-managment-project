package com.example.legal.document.generation;

import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface DocumentGenerationRepository extends Repository<DocumentGenerationEntity, Long> {

    DocumentGenerationEntity saveAndFlush(DocumentGenerationEntity generation);

    Optional<DocumentGenerationEntity> findById(Long id);

    Optional<DocumentGenerationEntity> findByIdempotencyKey(String idempotencyKey);
}
