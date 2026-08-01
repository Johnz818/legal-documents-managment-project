package com.example.legal.document.template;

import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface DocumentTemplateRepository extends Repository<DocumentTemplateEntity, Long> {

    DocumentTemplateEntity saveAndFlush(DocumentTemplateEntity template);

    Optional<DocumentTemplateEntity> findById(Long id);
}
