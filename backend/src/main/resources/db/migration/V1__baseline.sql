-- ==============================================================================
-- BuildSync Phase 1 - Baseline Migration
-- Validates Flyway migration execution and database connectivity
-- ==============================================================================

CREATE TABLE IF NOT EXISTS system_metadata (
    key VARCHAR(64) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_metadata (key, value)
VALUES ('foundation_phase', 'PHASE_1_COMPLETED')
ON CONFLICT (key) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;
