ALTER TABLE job_applications ADD COLUMN on_call_schedule VARCHAR(30);
ALTER TABLE job_applications ADD COLUMN on_call_details VARCHAR(200);
ALTER TABLE job_applications ADD COLUMN timezone VARCHAR(20);
ALTER TABLE job_applications ADD COLUMN timezone_other VARCHAR(50);
ALTER TABLE job_applications ADD COLUMN first_appeared DATE;
ALTER TABLE job_applications ADD COLUMN reposted_at DATE;
