-- Self-healing migration to ensure sync columns exist on repository table
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='repository' AND column_name='last_sync_duration') THEN
        ALTER TABLE repository ADD COLUMN last_sync_duration BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='repository' AND column_name='last_sync_error') THEN
        ALTER TABLE repository ADD COLUMN last_sync_error TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='repository' AND column_name='last_synced_at') THEN
        ALTER TABLE repository ADD COLUMN last_synced_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='repository' AND column_name='last_sync_status') THEN
        ALTER TABLE repository ADD COLUMN last_sync_status VARCHAR(255);
    END IF;
END $$;
