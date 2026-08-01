package com.example.legal.document.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(
        name = "document_template_fields",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_template_fields_key",
                        columnNames = {"template_version_id", "field_key"}
                ),
                @UniqueConstraint(
                        name = "uk_document_template_fields_order",
                        columnNames = {"template_version_id", "display_order"}
                )
        }
)
public class DocumentTemplateFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_version_id", nullable = false)
    private Long templateVersionId;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 30)
    private DocumentFieldValueType valueType;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_source", nullable = false, length = 30)
    private DocumentFieldDefaultSource defaultSource;

    @Column(name = "source_key", length = 100)
    private String sourceKey;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected DocumentTemplateFieldEntity() {
    }

    public DocumentTemplateFieldEntity(
            Long templateVersionId,
            String fieldKey,
            String displayName,
            String description,
            DocumentFieldValueType valueType,
            boolean required,
            DocumentFieldDefaultSource defaultSource,
            String sourceKey,
            int displayOrder
    ) {
        this.templateVersionId = templateVersionId;
        this.fieldKey = fieldKey;
        this.displayName = displayName;
        this.description = description;
        this.valueType = valueType;
        this.required = required;
        this.defaultSource = defaultSource;
        this.sourceKey = sourceKey;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getTemplateVersionId() {
        return templateVersionId;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public DocumentFieldValueType getValueType() {
        return valueType;
    }

    public boolean isRequired() {
        return required;
    }

    public DocumentFieldDefaultSource getDefaultSource() {
        return defaultSource;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
