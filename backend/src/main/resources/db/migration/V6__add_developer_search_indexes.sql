-- ==============================================================================
-- BuildSync Phase 3 Part 5 - Developer Search & Discoverability Indexes
-- Optimizes multi-criteria developer filtering and sorting
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_developer_profiles_experience_level ON developer_profiles (experience_level);
CREATE INDEX IF NOT EXISTS idx_developer_profiles_availability_status ON developer_profiles (availability_status);
CREATE INDEX IF NOT EXISTS idx_developer_profiles_location ON developer_profiles (location);
CREATE INDEX IF NOT EXISTS idx_developer_profiles_created_at ON developer_profiles (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_developer_profiles_updated_at ON developer_profiles (updated_at DESC);

