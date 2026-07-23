ALTER TABLE cases
    ADD COLUMN case_cause VARCHAR(255) NULL AFTER court_name,
    ADD COLUMN plaintiff VARCHAR(255) NULL AFTER case_cause,
    ADD COLUMN defendant VARCHAR(255) NULL AFTER plaintiff,
    ADD COLUMN filing_date DATE NULL AFTER lead_lawyer_name,
    ADD COLUMN hearing_date DATE NULL AFTER filing_date,
    ADD COLUMN judgment_date DATE NULL AFTER hearing_date,
    ADD COLUMN description TEXT NULL AFTER judgment_date;
