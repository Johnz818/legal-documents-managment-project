CREATE TABLE document_generations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    template_version_id BIGINT NOT NULL,
    case_document_id BIGINT NULL,
    case_status_snapshot VARCHAR(50) NOT NULL,
    idempotency_key CHAR(36) NOT NULL,
    request_sha256 CHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_document_generations PRIMARY KEY (id),
    CONSTRAINT uk_document_generations_case_document UNIQUE (case_document_id),
    CONSTRAINT uk_document_generations_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_document_generations_case FOREIGN KEY (case_id)
        REFERENCES cases (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_document_generations_template_version FOREIGN KEY (template_version_id)
        REFERENCES document_template_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_document_generations_case_document FOREIGN KEY (case_document_id)
        REFERENCES case_documents (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_document_generations_case_status CHECK (
        case_status_snapshot IN (
            'PENDING_FILING',
            'PRE_TRIAL_PREPARATION',
            'IN_TRIAL',
            'JUDGMENT_PENDING_APPEAL',
            'APPEAL_IN_PROGRESS',
            'FINAL_JUDGMENT',
            'IN_ENFORCEMENT',
            'CLOSED'
        )
    ),
    CONSTRAINT chk_document_generations_idempotency_key CHECK (
        REGEXP_LIKE(
            idempotency_key,
            '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
            'c'
        )
    ),
    CONSTRAINT chk_document_generations_request_sha256 CHECK (
        REGEXP_LIKE(request_sha256, '^[0-9a-f]{64}$', 'c')
    )
);

CREATE TABLE generation_values (
    id BIGINT NOT NULL AUTO_INCREMENT,
    generation_id BIGINT NOT NULL,
    template_field_id BIGINT NOT NULL,
    resolved_value MEDIUMTEXT NOT NULL,
    value_source VARCHAR(30) NOT NULL,
    CONSTRAINT pk_generation_values PRIMARY KEY (id),
    CONSTRAINT uk_generation_values_field UNIQUE (generation_id, template_field_id),
    CONSTRAINT fk_generation_values_generation FOREIGN KEY (generation_id)
        REFERENCES document_generations (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_generation_values_template_field FOREIGN KEY (template_field_id)
        REFERENCES document_template_fields (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_generation_values_source CHECK (
        value_source IN ('CASE_FIELD', 'SYSTEM_VALUE', 'USER_INPUT')
    )
);
