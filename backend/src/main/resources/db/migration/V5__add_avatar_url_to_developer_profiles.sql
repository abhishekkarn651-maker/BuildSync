-- ==============================================================================
-- BuildSync Phase 3 Part 4 - Avatar & Media Management Schema Migration
-- Adds avatar_url column to developer_profiles
-- ==============================================================================

ALTER TABLE developer_profiles
ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

