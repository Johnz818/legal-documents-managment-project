package com.example.legal.document.generation;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface GenerationValueRepository extends Repository<GenerationValueEntity, Long> {

    GenerationValueEntity save(GenerationValueEntity value);

    void flush();

    List<GenerationValueEntity> findAllByGenerationIdOrderById(Long generationId);
}
