-- ==============================================================================
-- BuildSync Phase 4 - Project Management Schema Migration
-- Creates:
--   1. projects (showcase, metadata, lifecycle status, visibility, owner)
--   2. project_technologies (many-to-many link to master skills catalogue)
-- ==============================================================================

-- 1. Projects Table
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    short_description VARCHAR(300) NOT NULL,
    detailed_description TEXT,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNING',
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    github_url VARCHAR(255),
    demo_url VARCHAR(255),
    start_date DATE,
    completion_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_projects_slug UNIQUE (slug),
    CONSTRAINT chk_project_status CHECK (status IN ('IDEA', 'PLANNING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'ARCHIVED')),
    CONSTRAINT chk_project_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT chk_project_dates CHECK (completion_date IS NULL OR start_date IS NULL OR completion_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_projects_owner_id ON projects (owner_id);
CREATE INDEX IF NOT EXISTS idx_projects_slug ON projects (slug);
CREATE INDEX IF NOT EXISTS idx_projects_visibility ON projects (visibility);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects (status);
CREATE INDEX IF NOT EXISTS idx_projects_category ON projects (category);
CREATE INDEX IF NOT EXISTS idx_projects_created_at ON projects (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_projects_updated_at ON projects (updated_at DESC);

-- 2. Project Technologies Association Table (linking to master skills catalogue)
CREATE TABLE IF NOT EXISTS project_technologies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_technologies UNIQUE (project_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_project_tech_project_id ON project_technologies (project_id);
CREATE INDEX IF NOT EXISTS idx_project_tech_skill_id ON project_technologies (skill_id);

