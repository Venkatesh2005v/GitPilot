-- Add owner and full_name columns to repository table safely for existing data.
-- The Repository entity now requires: owner (NOT NULL), full_name (NOT NULL, UNIQUE).
-- Existing rows must be backfilled before applying NOT NULL constraints.

-- 1. Add columns as NULLABLE first (safe for existing rows)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='repository' AND column_name='full_name') THEN
        ALTER TABLE repository ADD COLUMN full_name VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='repository' AND column_name='owner') THEN
        ALTER TABLE repository ADD COLUMN owner VARCHAR(255);
    END IF;
END $$;

-- 2. Backfill full_name.
--    Existing rows store the repository identifier in `name`, which is usually "owner/repo".
--    Use it directly when it already looks like a full name; otherwise derive from html_url.
UPDATE repository
SET full_name = CASE
    WHEN name LIKE '%/%' THEN name
    WHEN html_url IS NOT NULL AND html_url LIKE '%github.com/%'
        THEN regexp_replace(html_url, '^https?://github\.com/', '')
    ELSE name
END
WHERE full_name IS NULL;

-- 3. Backfill owner from full_name (portion before the first slash), else from html_url.
UPDATE repository
SET owner = CASE
    WHEN full_name LIKE '%/%' THEN split_part(full_name, '/', 1)
    WHEN html_url IS NOT NULL AND html_url LIKE '%github.com/%'
        THEN split_part(regexp_replace(html_url, '^https?://github\.com/', ''), '/', 1)
    ELSE 'unknown'
END
WHERE owner IS NULL;

-- 4. Guarantee no NULLs remain (defensive fallback for any unexpected rows)
UPDATE repository SET full_name = 'unknown/' || id WHERE full_name IS NULL;
UPDATE repository SET owner = 'unknown' WHERE owner IS NULL;

-- 5. Apply NOT NULL constraints now that all rows are populated
ALTER TABLE repository ALTER COLUMN full_name SET NOT NULL;
ALTER TABLE repository ALTER COLUMN owner SET NOT NULL;

-- 6. Add unique constraint on full_name (entity declares unique = true).
--    Skip if a conflicting unique index/constraint already exists.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_repository_full_name'
    ) THEN
        ALTER TABLE repository ADD CONSTRAINT uq_repository_full_name UNIQUE (full_name);
    END IF;
END $$;
