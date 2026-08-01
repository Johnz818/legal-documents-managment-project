package com.example.legal.document.template;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface DocumentTemplateFieldRepository extends Repository<DocumentTemplateFieldEntity, Long> {

    DocumentTemplateFieldEntity save(DocumentTemplateFieldEntity field);

    DocumentTemplateFieldEntity saveAndFlush(DocumentTemplateFieldEntity field);

    void flush();

    List<DocumentTemplateFieldEntity> findAllByTemplateVersionIdOrderByDisplayOrder(Long templateVersionId);

    Optional<DocumentTemplateFieldEntity> findByTemplateVersionIdAndFieldKey(
            Long templateVersionId,
            String fieldKey
    );
}
