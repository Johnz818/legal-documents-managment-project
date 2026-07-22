CREATE TABLE cases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_number VARCHAR(100) NOT NULL,
    case_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    court_name VARCHAR(255) NULL,
    lead_lawyer_name VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_cases PRIMARY KEY (id),
    CONSTRAINT uk_cases_case_number UNIQUE (case_number),
    CONSTRAINT chk_cases_status CHECK (
        status IN (
            'PENDING_FILING',
            'PRE_TRIAL_PREPARATION',
            'IN_TRIAL',
            'CLOSED'
        )
    )
);
