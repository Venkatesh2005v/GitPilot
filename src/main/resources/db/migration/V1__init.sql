CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    github_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    avatar_url VARCHAR(255) NOT NULL,
    profile_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE repository (
    id BIGSERIAL PRIMARY KEY,
    github_repo_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    default_branch VARCHAR(255),
    html_url VARCHAR(255),
    selected BOOLEAN DEFAULT FALSE,
    private_repo BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_repository_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE commits (
    id BIGSERIAL PRIMARY KEY,
    github_commit_sha VARCHAR(255) UNIQUE NOT NULL,
    message TEXT,
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    commit_date TIMESTAMP,
    commit_url VARCHAR(255),
    repository_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_commits_repository FOREIGN KEY (repository_id) REFERENCES repository(id)
);
