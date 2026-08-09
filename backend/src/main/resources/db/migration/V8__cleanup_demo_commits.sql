-- Cleanup: Remove demo/mock commits from repositories 3 and 4.
-- These were inserted by a previous synchronization fallback that used placeholder author names.

DELETE FROM commit
WHERE repository_id IN (3, 4)
  AND (
    author_name IN ('Alice Developer', 'Bob Architect', 'Charlie QA')
    OR github_commit_sha = 'mocksha'
  );

-- Also remove any seeded recommendations for these repos so fresh ones regenerate
DELETE FROM recommendation
WHERE repository_id IN (3, 4);
