-- Fix nullable mismatch: entity expects work_type to be NOT NULL
-- All existing rows must have work_type set (JobForm requires @NotNull)
ALTER TABLE job_applications ALTER COLUMN work_type SET NOT NULL;