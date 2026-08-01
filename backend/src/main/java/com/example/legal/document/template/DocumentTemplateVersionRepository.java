package com.example.legal.document.template;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface DocumentTemplateVersionRepository extends Repository<DocumentTemplateVersionEntity, Long> {

    DocumentTemplateVersionEntity save(DocumentTemplateVersionEntity version);

    DocumentTemplateVersionEntity saveAndFlush(DocumentTemplateVersionEntity version);

    void flush();

    List<DocumentTemplateVersionEntity> findAllByTemplateIdOrderByVersionNumberDesc(Long templateId);

    Optional<DocumentTemplateVersionEntity> findByIdAndTemplateId(Long id, Long templateId);
}
