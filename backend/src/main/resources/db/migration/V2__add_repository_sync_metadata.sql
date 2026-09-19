-- Idempotent: safe to run whether or not the columns already exist
-- (existing local databases may have been created by Hibernate ddl-auto).
ALTER TABLE repository ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;
ALTER TABLE repository ADD COLUMN IF NOT EXISTS last_sync_status VARCHAR(255);
ALTER TABLE repository ADD COLUMN IF NOT EXISTS last_sync_duration BIGINT;
ALTER TABLE repository ADD COLUMN IF NOT EXISTS last_sync_error TEXT;
