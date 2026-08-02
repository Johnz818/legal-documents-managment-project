package com.example.legal.document.template;

import org.springframework.data.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DocumentTemplateVersionRepository extends Repository<DocumentTemplateVersionEntity, Long> {

    DocumentTemplateVersionEntity save(DocumentTemplateVersionEntity version);

    DocumentTemplateVersionEntity saveAndFlush(DocumentTemplateVersionEntity version);

    void flush();

    List<DocumentTemplateVersionEntity> findAllByTemplateIdOrderByVersionNumberDesc(Long templateId);

    Optional<DocumentTemplateVersionEntity> findByIdAndTemplateId(Long id, Long templateId);

    Optional<DocumentTemplateVersionEntity> findByTemplateIdAndVersionNumber(
            Long templateId,
            int versionNumber
    );

    Optional<DocumentTemplateVersionEntity> findTopByTemplateIdOrderByVersionNumberDesc(Long templateId);

    Page<DocumentTemplateVersionEntity> findAllByTemplateIdOrderByVersionNumberDesc(
            Long templateId,
            Pageable pageable
    );
}
