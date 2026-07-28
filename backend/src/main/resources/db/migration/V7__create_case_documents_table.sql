CREATE TABLE case_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    document_source VARCHAR(30) NOT NULL,
    file_format VARCHAR(20) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_case_documents PRIMARY KEY (id),
    CONSTRAINT uk_case_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_case_documents_case FOREIGN KEY (case_id)
        REFERENCES cases (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_case_documents_source CHECK (
        document_source IN ('UPLOADED', 'GENERATED')
    ),
    CONSTRAINT chk_case_documents_format CHECK (
        file_format IN ('PDF', 'DOC', 'DOCX')
    ),
    CONSTRAINT chk_case_documents_file_size CHECK (file_size >= 0),
    INDEX idx_case_documents_case_id (case_id)
);
