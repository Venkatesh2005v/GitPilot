-- Performance Optimization Indexes (idempotent)
CREATE INDEX IF NOT EXISTS idx_repository_user_id ON repository(user_id);
CREATE INDEX IF NOT EXISTS idx_commits_repository_id ON commits(repository_id);
CREATE INDEX IF NOT EXISTS idx_ai_reports_repository_id ON ai_reports(repository_id);
