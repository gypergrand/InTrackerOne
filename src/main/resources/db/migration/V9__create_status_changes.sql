CREATE TABLE status_changes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_application_id BIGINT NOT NULL,
    from_status_id BIGINT,
    to_status_id BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    decided_by VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sc_job FOREIGN KEY (job_application_id) REFERENCES job_applications(id),
    CONSTRAINT fk_sc_from_status FOREIGN KEY (from_status_id) REFERENCES pipeline_statuses(id),
    CONSTRAINT fk_sc_to_status FOREIGN KEY (to_status_id) REFERENCES pipeline_statuses(id)
);
