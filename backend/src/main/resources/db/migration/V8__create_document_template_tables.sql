CREATE TABLE document_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    template_type VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_document_templates PRIMARY KEY (id),
    CONSTRAINT chk_document_templates_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_document_templates_type CHECK (
        template_type IN ('PRESET', 'CUSTOM')
    )
);

CREATE TABLE document_template_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    published_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_document_template_versions PRIMARY KEY (id),
    CONSTRAINT uk_document_template_versions_number UNIQUE (template_id, version_number),
    CONSTRAINT uk_document_template_versions_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_document_template_versions_template FOREIGN KEY (template_id)
        REFERENCES document_templates (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_document_template_versions_number CHECK (version_number > 0),
    CONSTRAINT chk_document_template_versions_file_name CHECK (
        CHAR_LENGTH(TRIM(original_file_name)) > 0
    ),
    CONSTRAINT chk_document_template_versions_storage_key CHECK (
        CHAR_LENGTH(TRIM(storage_key)) > 0
    ),
    CONSTRAINT chk_document_template_versions_content_type CHECK (
        CHAR_LENGTH(TRIM(content_type)) > 0
    ),
    CONSTRAINT chk_document_template_versions_file_size CHECK (file_size >= 0),
    CONSTRAINT chk_document_template_versions_sha256 CHECK (
        REGEXP_LIKE(content_sha256, '^[0-9a-f]{64}$', 'c')
    )
);

CREATE TABLE document_template_fields (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_version_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    value_type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL,
    default_source VARCHAR(30) NOT NULL,
    source_key VARCHAR(100) NULL,
    display_order INT NOT NULL,
    CONSTRAINT pk_document_template_fields PRIMARY KEY (id),
    CONSTRAINT uk_document_template_fields_key UNIQUE (template_version_id, field_key),
    CONSTRAINT uk_document_template_fields_order UNIQUE (template_version_id, display_order),
    CONSTRAINT fk_document_template_fields_version FOREIGN KEY (template_version_id)
        REFERENCES document_template_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_document_template_fields_key CHECK (
        CHAR_LENGTH(TRIM(field_key)) > 0
    ),
    CONSTRAINT chk_document_template_fields_display_name CHECK (
        CHAR_LENGTH(TRIM(display_name)) > 0
    ),
    CONSTRAINT chk_document_template_fields_value_type CHECK (
        value_type IN ('TEXT', 'DATE', 'DECIMAL', 'BOOLEAN')
    ),
    CONSTRAINT chk_document_template_fields_default_source CHECK (
        default_source IN ('CASE_FIELD', 'SYSTEM_VALUE', 'USER_INPUT')
    ),
    CONSTRAINT chk_document_template_fields_source_key CHECK (
        (default_source IN ('CASE_FIELD', 'SYSTEM_VALUE')
            AND source_key IS NOT NULL
            AND CHAR_LENGTH(TRIM(source_key)) > 0)
        OR (default_source = 'USER_INPUT' AND source_key IS NULL)
    ),
    CONSTRAINT chk_document_template_fields_display_order CHECK (display_order >= 0)
);
