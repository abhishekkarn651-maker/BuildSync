-- ============================================================================
-- Migration: V8__create_collaboration_and_team_tables.sql
-- Description: Creates project_members and collaboration_requests tables,
--              constraints, partial indexes, and lifecycle triggers.
-- ============================================================================

-- 1. Create project_members table
CREATE TABLE IF NOT EXISTS project_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_members_role CHECK (role IN ('MEMBER', 'CONTRIBUTOR', 'MAINTAINER'))
);

-- Performance indexes for project_members
CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id ON project_members(user_id);
CREATE INDEX IF NOT EXISTS idx_project_members_joined_at ON project_members(joined_at DESC);

-- 2. Create collaboration_requests table
CREATE TABLE IF NOT EXISTS collaboration_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMPTZ,
    CONSTRAINT chk_collab_request_type CHECK (request_type IN ('JOIN_REQUEST', 'INVITATION')),
    CONSTRAINT chk_collab_request_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_collab_different_users CHECK (requester_id <> recipient_id)
);

-- Invariant: Only one active PENDING request between the same participants for a project
CREATE UNIQUE INDEX IF NOT EXISTS uq_pending_join_request
    ON collaboration_requests (project_id, requester_id)
    WHERE status = 'PENDING' AND request_type = 'JOIN_REQUEST';

CREATE UNIQUE INDEX IF NOT EXISTS uq_pending_invitation
    ON collaboration_requests (project_id, recipient_id)
    WHERE status = 'PENDING' AND request_type = 'INVITATION';

-- Performance indexes for collaboration queries
CREATE INDEX IF NOT EXISTS idx_collab_recipient_status ON collaboration_requests(recipient_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_collab_requester_status ON collaboration_requests(requester_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_collab_project_id ON collaboration_requests(project_id, status);

-- 3. Trigger function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to maintain updated_at on collaboration_requests
CREATE OR REPLACE TRIGGER set_collaboration_requests_timestamp
    BEFORE UPDATE ON collaboration_requests
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_timestamp();

