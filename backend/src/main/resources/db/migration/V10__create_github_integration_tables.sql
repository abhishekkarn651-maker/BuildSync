-- ============================================================================
-- Migration: V10__create_github_integration_tables.sql
-- Description: Creates tables for GitHub App OAuth authorization, encrypted
--              credentials storage, repository metadata, and project linking.
-- ============================================================================

-- 1. GitHub OAuth States (Server-side PKCE & CSRF state store)
CREATE TABLE IF NOT EXISTS github_oauth_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_hash VARCHAR(64) NOT NULL,
    code_verifier VARCHAR(128) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_github_oauth_states_hash UNIQUE (state_hash)
);

CREATE INDEX IF NOT EXISTS idx_github_oauth_states_hash ON github_oauth_states(state_hash);
CREATE INDEX IF NOT EXISTS idx_github_oauth_states_expires_at ON github_oauth_states(expires_at);
CREATE INDEX IF NOT EXISTS idx_github_oauth_states_user_id ON github_oauth_states(user_id);

-- 2. GitHub Connections (Encrypted credentials & account identity)
CREATE TABLE IF NOT EXISTS github_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_user_id BIGINT NOT NULL,
    github_username VARCHAR(100) NOT NULL,
    github_avatar_url VARCHAR(500),
    github_profile_url VARCHAR(255),
    encrypted_access_token TEXT NOT NULL,
    encrypted_refresh_token TEXT,
    access_token_expires_at TIMESTAMPTZ,
    refresh_token_expires_at TIMESTAMPTZ,
    connection_status VARCHAR(30) NOT NULL DEFAULT 'CONNECTED',
    connected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_github_connections_user_id UNIQUE (user_id),
    CONSTRAINT uq_github_connections_github_user_id UNIQUE (github_user_id),
    CONSTRAINT chk_github_connection_status CHECK (connection_status IN ('CONNECTED', 'DISCONNECTED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_github_connections_user_id ON github_connections(user_id);
CREATE INDEX IF NOT EXISTS idx_github_connections_github_user_id ON github_connections(github_user_id);

CREATE OR REPLACE TRIGGER set_github_connections_timestamp
    BEFORE UPDATE ON github_connections
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

-- 3. GitHub Repositories (Imported metadata)
CREATE TABLE IF NOT EXISTS github_repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_connection_id UUID NOT NULL REFERENCES github_connections(id) ON DELETE CASCADE,
    github_repository_id BIGINT NOT NULL,
    owner_login VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    html_url VARCHAR(255) NOT NULL,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    default_branch VARCHAR(100) DEFAULT 'main',
    primary_language VARCHAR(50),
    stars_count INTEGER NOT NULL DEFAULT 0,
    forks_count INTEGER NOT NULL DEFAULT 0,
    open_issues_count INTEGER NOT NULL DEFAULT 0,
    is_showcased BOOLEAN NOT NULL DEFAULT FALSE,
    pushed_at TIMESTAMPTZ,
    github_created_at TIMESTAMPTZ,
    github_updated_at TIMESTAMPTZ,
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_github_repo_conn_repoid UNIQUE (github_connection_id, github_repository_id),
    CONSTRAINT chk_github_repo_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'INTERNAL'))
);

CREATE INDEX IF NOT EXISTS idx_github_repos_conn_id ON github_repositories(github_connection_id);
CREATE INDEX IF NOT EXISTS idx_github_repos_repoid ON github_repositories(github_repository_id);
CREATE INDEX IF NOT EXISTS idx_github_repos_language ON github_repositories(primary_language);
CREATE INDEX IF NOT EXISTS idx_github_repos_stars ON github_repositories(stars_count DESC);
CREATE INDEX IF NOT EXISTS idx_github_repos_pushed_at ON github_repositories(pushed_at DESC);
CREATE INDEX IF NOT EXISTS idx_github_repos_showcased ON github_repositories(github_connection_id, is_showcased);

CREATE OR REPLACE TRIGGER set_github_repositories_timestamp
    BEFORE UPDATE ON github_repositories
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

-- 4. Project GitHub Repositories (Linking projects to imported repos)
CREATE TABLE IF NOT EXISTS project_github_repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    github_repository_id UUID NOT NULL REFERENCES github_repositories(id) ON DELETE CASCADE,
    linked_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_github_repo UNIQUE (project_id, github_repository_id)
);

CREATE INDEX IF NOT EXISTS idx_proj_github_repos_proj_id ON project_github_repositories(project_id);
CREATE INDEX IF NOT EXISTS idx_proj_github_repos_repo_id ON project_github_repositories(github_repository_id);
CREATE INDEX IF NOT EXISTS idx_proj_github_repos_linked_by ON project_github_repositories(linked_by_user_id);

