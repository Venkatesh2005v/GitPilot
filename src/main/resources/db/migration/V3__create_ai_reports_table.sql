CREATE TABLE ai_reports (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    report_type VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    fallback_used VARCHAR(255),
    generated_report TEXT NOT NULL,
    generated_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_ai_reports_repository FOREIGN KEY (repository_id) REFERENCES repository(id)
);
