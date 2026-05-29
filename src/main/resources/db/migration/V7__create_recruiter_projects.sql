CREATE TABLE recruiter_projects (
    recruiter_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    PRIMARY KEY (recruiter_id, project_id),
    CONSTRAINT fk_rp_recruiter FOREIGN KEY (recruiter_id) REFERENCES recruiters(id),
    CONSTRAINT fk_rp_project FOREIGN KEY (project_id) REFERENCES projects(id)
);
