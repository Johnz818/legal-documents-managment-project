package com.example.legal.document.generation;

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
        name = "generation_values",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_generation_values_field",
                columnNames = {"generation_id", "template_field_id"}
        )
)
public class GenerationValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generation_id", nullable = false)
    private Long generationId;

    @Column(name = "template_field_id", nullable = false)
    private Long templateFieldId;

    @Column(name = "resolved_value", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String resolvedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_source", nullable = false, length = 30)
    private GenerationValueSource valueSource;

    protected GenerationValueEntity() {
    }

    public GenerationValueEntity(
            Long generationId,
            Long templateFieldId,
            String resolvedValue,
            GenerationValueSource valueSource
    ) {
        this.generationId = generationId;
        this.templateFieldId = templateFieldId;
        this.resolvedValue = resolvedValue;
        this.valueSource = valueSource;
    }

    public Long getId() {
        return id;
    }

    public Long getGenerationId() {
        return generationId;
    }

    public Long getTemplateFieldId() {
        return templateFieldId;
    }

    public String getResolvedValue() {
        return resolvedValue;
    }

    public GenerationValueSource getValueSource() {
        return valueSource;
    }
}
