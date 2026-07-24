ALTER TABLE cases
    DROP CHECK chk_cases_status,
    ADD CONSTRAINT chk_cases_status CHECK (
        status IN (
            'PENDING_FILING',
            'PRE_TRIAL_PREPARATION',
            'IN_TRIAL',
            'JUDGMENT_PENDING_APPEAL',
            'APPEAL_IN_PROGRESS',
            'FINAL_JUDGMENT',
            'IN_ENFORCEMENT',
            'CLOSED'
        )
    );
