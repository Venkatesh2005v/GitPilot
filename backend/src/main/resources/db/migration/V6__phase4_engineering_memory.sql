-- Phase 4: GitPilot Engineering Memory Platform Schema Migration

CREATE TABLE IF NOT EXISTS repository_journeys (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    milestone_name VARCHAR(255) NOT NULL,
    milestone_category VARCHAR(100),
    icon_name VARCHAR(100),
    description TEXT,
    event_date TIMESTAMP NOT NULL,
    release_version VARCHAR(50),
    contributor VARCHAR(255),
    month_val VARCHAR(20),
    year_val INT
);

CREATE TABLE IF NOT EXISTS engineering_timelines (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    time_period VARCHAR(100) NOT NULL,
    milestone_title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    impact_level VARCHAR(50),
    commit_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS repository_dnas (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    activity_level INT NOT NULL,
    repository_size INT NOT NULL,
    architecture_quality INT NOT NULL,
    testing_strength INT NOT NULL,
    documentation_quality INT NOT NULL,
    deployment_readiness INT NOT NULL,
    risk_level INT NOT NULL,
    maintainability INT NOT NULL,
    knowledge_score INT NOT NULL,
    personality_archetype VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS onboarding_steps (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    module_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    why_it_matters TEXT NOT NULL,
    reading_time_minutes INT NOT NULL,
    difficulty VARCHAR(50),
    dependencies_json TEXT,
    key_files_json TEXT
);

CREATE TABLE IF NOT EXISTS knowledge_nodes (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    node_key VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    complexity_score INT DEFAULT 50
);

CREATE TABLE IF NOT EXISTS knowledge_edges (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    source_node_key VARCHAR(100) NOT NULL,
    target_node_key VARCHAR(100) NOT NULL,
    relationship_type VARCHAR(100),
    label VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS engineering_decisions (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    decision_title VARCHAR(255) NOT NULL,
    rationale TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ACCEPTED',
    category VARCHAR(100),
    date_inferred TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    commit_sha VARCHAR(100),
    impact_summary TEXT
);

CREATE TABLE IF NOT EXISTS contextual_recommendations (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    priority VARCHAR(50),
    reason TEXT NOT NULL,
    estimated_effort VARCHAR(50),
    expected_impact VARCHAR(255),
    target_module VARCHAR(255),
    action_taken BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_repo_journey_repo ON repository_journeys(repository_id);
CREATE INDEX IF NOT EXISTS idx_repo_timeline_repo ON engineering_timelines(repository_id);
CREATE INDEX IF NOT EXISTS idx_repo_dna_repo ON repository_dnas(repository_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_repo ON onboarding_steps(repository_id);
CREATE INDEX IF NOT EXISTS idx_kn_node_repo ON knowledge_nodes(repository_id);
CREATE INDEX IF NOT EXISTS idx_kn_edge_repo ON knowledge_edges(repository_id);
CREATE INDEX IF NOT EXISTS idx_decisions_repo ON engineering_decisions(repository_id);
CREATE INDEX IF NOT EXISTS idx_context_recs_repo ON contextual_recommendations(repository_id);
