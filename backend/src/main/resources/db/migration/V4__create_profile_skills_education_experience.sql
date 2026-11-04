-- ==============================================================================
-- BuildSync Phase 3 Part 3 - Skills, Education & Experience Schema Migration
-- Creates tables:
--   1. skills (catalogue)
--   2. developer_profile_skills (profile association)
--   3. profile_education (education history)
--   4. profile_experience (work experience)
-- ==============================================================================

-- 1. Master Skills Catalogue Table
CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_skills_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_skills_name ON skills (name);
CREATE INDEX IF NOT EXISTS idx_skills_category ON skills (category);

-- 2. Developer Profile Skills Association Table
CREATE TABLE IF NOT EXISTS developer_profile_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES developer_profiles (id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
    proficiency_level VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_developer_profile_skills UNIQUE (profile_id, skill_id),
    CONSTRAINT chk_skill_proficiency CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'))
);

CREATE INDEX IF NOT EXISTS idx_dev_profile_skills_profile_id ON developer_profile_skills (profile_id);
CREATE INDEX IF NOT EXISTS idx_dev_profile_skills_skill_id ON developer_profile_skills (skill_id);

-- 3. Developer Profile Education Table
CREATE TABLE IF NOT EXISTS profile_education (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES developer_profiles (id) ON DELETE CASCADE,
    institution VARCHAR(200) NOT NULL,
    degree VARCHAR(150),
    field_of_study VARCHAR(150),
    start_date DATE,
    end_date DATE,
    currently_studying BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_education_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_education_current CHECK (NOT currently_studying OR end_date IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_profile_education_profile_id ON profile_education (profile_id);

-- 4. Developer Profile Experience Table
CREATE TABLE IF NOT EXISTS profile_experience (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES developer_profiles (id) ON DELETE CASCADE,
    job_title VARCHAR(150) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    employment_type VARCHAR(50),
    location VARCHAR(150),
    start_date DATE,
    end_date DATE,
    currently_working BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_experience_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_experience_current CHECK (NOT currently_working OR end_date IS NULL),
    CONSTRAINT chk_experience_emp_type CHECK (
        employment_type IS NULL OR employment_type IN (
            'FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'CONTRACT', 'FREELANCE', 'SELF_EMPLOYED'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_profile_experience_profile_id ON profile_experience (profile_id);

-- 5. Seed Initial Master Skills Catalogue (Idempotent)
INSERT INTO skills (name, category)
VALUES
    -- Programming Languages
    ('Java', 'PROGRAMMING_LANGUAGE'),
    ('Python', 'PROGRAMMING_LANGUAGE'),
    ('JavaScript', 'PROGRAMMING_LANGUAGE'),
    ('TypeScript', 'PROGRAMMING_LANGUAGE'),
    ('C', 'PROGRAMMING_LANGUAGE'),
    ('C++', 'PROGRAMMING_LANGUAGE'),
    ('Go', 'PROGRAMMING_LANGUAGE'),
    ('Rust', 'PROGRAMMING_LANGUAGE'),
    ('PHP', 'PROGRAMMING_LANGUAGE'),
    ('Kotlin', 'PROGRAMMING_LANGUAGE'),

    -- Frameworks
    ('Spring Boot', 'FRAMEWORK'),
    ('React', 'FRAMEWORK'),
    ('Next.js', 'FRAMEWORK'),
    ('Angular', 'FRAMEWORK'),
    ('Vue.js', 'FRAMEWORK'),
    ('Node.js', 'FRAMEWORK'),
    ('Express', 'FRAMEWORK'),
    ('Django', 'FRAMEWORK'),
    ('FastAPI', 'FRAMEWORK'),

    -- Databases
    ('PostgreSQL', 'DATABASE'),
    ('MySQL', 'DATABASE'),
    ('MongoDB', 'DATABASE'),
    ('Redis', 'DATABASE'),
    ('SQLite', 'DATABASE'),

    -- Tools and Technologies
    ('Git', 'TOOL'),
    ('GitHub', 'TOOL'),
    ('Postman', 'TOOL'),
    ('Docker', 'DEVOPS'),
    ('Kubernetes', 'DEVOPS'),
    ('AWS', 'DEVOPS'),
    ('Linux', 'DEVOPS'),
    ('Figma', 'DESIGN')
ON CONFLICT (name) DO NOTHING;

