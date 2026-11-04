-- ==============================================================================
-- BuildSync Phase 3 Part 1 - Developer Profile Schema Migration
-- Creates table: developer_profiles
-- ==============================================================================

CREATE TABLE IF NOT EXISTS developer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    headline VARCHAR(150),
    bio VARCHAR(500),
    about TEXT,
    location VARCHAR(100),
    website_url VARCHAR(255),
    github_url VARCHAR(255),
    linkedin_url VARCHAR(255),
    twitter_url VARCHAR(255),
    experience_level VARCHAR(30),
    years_of_experience INTEGER,
    availability_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_developer_profiles_user_id UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_developer_profiles_user_id ON developer_profiles (user_id);

