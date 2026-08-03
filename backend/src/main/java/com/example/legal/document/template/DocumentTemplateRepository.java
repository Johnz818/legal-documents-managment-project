package com.example.legal.document.template;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DocumentTemplateRepository extends Repository<DocumentTemplateEntity, Long> {

    DocumentTemplateEntity saveAndFlush(DocumentTemplateEntity template);

    Optional<DocumentTemplateEntity> findById(Long id);

    Page<DocumentTemplateEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select template from DocumentTemplateEntity template where template.id = :id")
    Optional<DocumentTemplateEntity> findByIdForUpdate(@Param("id") Long id);
}
