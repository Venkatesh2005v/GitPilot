ALTER TABLE repository ADD COLUMN last_synced_at TIMESTAMP;
ALTER TABLE repository ADD COLUMN last_sync_status VARCHAR(255);
ALTER TABLE repository ADD COLUMN last_sync_duration BIGINT;
ALTER TABLE repository ADD COLUMN last_sync_error TEXT;
