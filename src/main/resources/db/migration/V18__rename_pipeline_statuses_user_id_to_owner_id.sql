-- Rename user_id to owner_id for consistency with all other entities.
-- Both H2 and PostgreSQL support this syntax and automatically update
-- constraint references (FK, UNIQUE) to use the new column name.
ALTER TABLE pipeline_statuses RENAME COLUMN user_id TO owner_id;
