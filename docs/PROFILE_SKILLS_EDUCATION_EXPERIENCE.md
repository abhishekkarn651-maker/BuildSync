# BuildSync — Skills, Education & Professional Experience Documentation
**Phase 3, Part 3/5: Skills, Education & Professional Experience Management**  
**Technology Stack:** Java 17 LTS, Spring Boot 3.4.3, Spring Security 6, Spring Data JPA, PostgreSQL 17, Flyway 10, Next.js 16.4.3, React 19, TypeScript, Tailwind CSS 4, Cyber Ember Red (`#05`) Design System.

---

## 1. Executive Summary & Architecture Overview

BuildSync Phase 3 Part 3 extends the Developer Profile System with comprehensive tracking for technical capabilities, academic background, and industry track record:
1. **Master Skills Catalogue & Profile Skills Assignment**:
   - Master catalogue of curated technical skills categorized into programming languages, frameworks, databases, DevOps tools, design tools, and utilities.
   - Profile skill linking with granular proficiency indicators (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`) and unique constraint prevention against duplicate skill additions.
2. **Education History Management**:
   - Structured academic credential tracking including institutions, degrees, fields of study, date ranges, and active enrollment flags.
   - Automatic database-level check constraints enforcing start date <= end date, and `end_date IS NULL` when currently studying.
3. **Professional Work Experience Management**:
   - Career timeline tracking with employment types (`FULL_TIME`, `PART_TIME`, `INTERNSHIP`, `CONTRACT`, `FREELANCE`, `SELF_EMPLOYED`), roles, organizations, locations, and descriptions.
   - Database-level check constraints enforcing date bounds and `end_date IS NULL` when currently working.
4. **Unified Profile Aggregation**:
   - Extended `GET /api/v1/profiles/{username}` to automatically aggregate the developer's basic profile, verified skills with proficiency, work history sorted chronologically, and education credentials sorted chronologically.
5. **Full-Stack Security & Ownership**:
   - Ownership of skills, education, and experience is strictly derived from the authenticated user's JWT access token (`jwt.getSubject()`). Tampering with or modifying records belonging to other developers is mathematically prevented.

---

## 2. Database Schema & Flyway Migration

### Migration File: `V4__create_profile_skills_education_experience.sql`
**Path:** `backend/src/main/resources/db/migration/V4__create_profile_skills_education_experience.sql`

```sql
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

-- 2. Seed Master Skills Catalogue
INSERT INTO skills (name, category) VALUES
    ('Java', 'PROGRAMMING_LANGUAGE'),
    ('Python', 'PROGRAMMING_LANGUAGE'),
    ('TypeScript', 'PROGRAMMING_LANGUAGE'),
    ('JavaScript', 'PROGRAMMING_LANGUAGE'),
    ('Go', 'PROGRAMMING_LANGUAGE'),
    ('Rust', 'PROGRAMMING_LANGUAGE'),
    ('C++', 'PROGRAMMING_LANGUAGE'),
    ('C', 'PROGRAMMING_LANGUAGE'),
    ('Kotlin', 'PROGRAMMING_LANGUAGE'),
    ('PHP', 'PROGRAMMING_LANGUAGE'),
    ('React', 'FRAMEWORK'),
    ('Next.js', 'FRAMEWORK'),
    ('Spring Boot', 'FRAMEWORK'),
    ('Node.js', 'FRAMEWORK'),
    ('Express', 'FRAMEWORK'),
    ('Django', 'FRAMEWORK'),
    ('FastAPI', 'FRAMEWORK'),
    ('Angular', 'FRAMEWORK'),
    ('Vue.js', 'FRAMEWORK'),
    ('PostgreSQL', 'DATABASE'),
    ('MySQL', 'DATABASE'),
    ('MongoDB', 'DATABASE'),
    ('Redis', 'DATABASE'),
    ('SQLite', 'DATABASE'),
    ('Docker', 'DEVOPS'),
    ('Kubernetes', 'DEVOPS'),
    ('AWS', 'DEVOPS'),
    ('Linux', 'DEVOPS'),
    ('Git', 'TOOL'),
    ('GitHub', 'TOOL'),
    ('Postman', 'TOOL'),
    ('Figma', 'DESIGN')
ON CONFLICT (name) DO NOTHING;

-- 3. Developer Profile Skills Mapping Table
CREATE TABLE IF NOT EXISTS developer_profile_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES developer_profiles (id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
    proficiency_level VARCHAR(30) NOT NULL DEFAULT 'INTERMEDIATE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_profile_skill UNIQUE (profile_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_profile_skills_profile_id ON developer_profile_skills (profile_id);
CREATE INDEX IF NOT EXISTS idx_profile_skills_skill_id ON developer_profile_skills (skill_id);

-- 4. Education Table
CREATE TABLE IF NOT EXISTS profile_education (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES developer_profiles (id) ON DELETE CASCADE,
    institution VARCHAR(200) NOT NULL,
    degree VARCHAR(150),
    field_of_study VARCHAR(150),
    start_date DATE,
    end_date DATE,
    currently_studying BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_education_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_education_current CHECK (NOT currently_studying OR end_date IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_profile_education_profile_id ON profile_education (profile_id);

-- 5. Professional Work Experience Table
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
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_experience_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_experience_current CHECK (NOT currently_working OR end_date IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_profile_experience_profile_id ON profile_experience (profile_id);
```

---

## 3. Backend Implementation Details

### 3.1 Domain Enums & Entities
- **`SkillCategory`**: `PROGRAMMING_LANGUAGE`, `FRAMEWORK`, `DATABASE`, `DEVOPS`, `TOOL`, `DESIGN`, `OTHER`.
- **`ProficiencyLevel`**: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`.
- **`EmploymentType`**: `FULL_TIME`, `PART_TIME`, `INTERNSHIP`, `CONTRACT`, `FREELANCE`, `SELF_EMPLOYED`.
- **`Skill`**: Master skill catalogue entity with unique name and category.
- **`DeveloperProfileSkill`**: Association entity linking `DeveloperProfile` and `Skill` with `ProficiencyLevel`.
- **`Education`**: Entity capturing academic degree, institution, field of study, timeline, and current enrollment.
- **`WorkExperience`**: Entity capturing job title, company name, employment type, dates, and active role status.

### 3.2 Repositories
- **`SkillRepository`**:
  - `findAllByOrderByNameAsc()`
  - `findByCategoryOrderByNameAsc(SkillCategory category)`
  - `findByNameContainingIgnoreCaseOrderByNameAsc(String name)`
  - `findByCategoryAndNameContainingIgnoreCaseOrderByNameAsc(SkillCategory category, String name)`
- **`DeveloperProfileSkillRepository`**:
  - `findByProfileIdWithSkill(UUID profileId)` (Fetches skills and categories with JOIN FETCH)
  - `findByProfileIdAndSkillId(UUID profileId, UUID skillId)`
  - `existsByProfileIdAndSkillId(UUID profileId, UUID skillId)`
  - `deleteByProfileIdAndSkillId(UUID profileId, UUID skillId)`
- **`EducationRepository`**:
  - `findByProfileIdOrderByStartDateDesc(UUID profileId)`
- **`ExperienceRepository`**:
  - `findByProfileIdOrderByStartDateDesc(UUID profileId)`

### 3.3 Application Services
- **`SkillService`**:
  - Catalogue query search & filtering without native SQL quirks.
  - Adding skills to the authenticated profile with duplicate prevention.
  - Updating proficiency level (`PUT /api/v1/profiles/me/skills/{skillId}`).
  - Deleting skills from the profile (`DELETE /api/v1/profiles/me/skills/{skillId}`).
- **`EducationService`**:
  - Adding, updating, retrieving, and deleting education records.
  - Profile ownership validation ensuring users only manipulate their own education history.
- **`ExperienceService`**:
  - Adding, updating, retrieving, and deleting work experience records.
  - Strict profile ownership enforcement.
- **`DeveloperProfileService`**:
  - Enhanced `getProfileByUsername` and `getProfileByUserId` to eagerly assemble skills, education, and work experience into the consolidated `ProfileResponse`.

---

## 4. REST API Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/skills` | Public | Search master catalogue by `?search=...` and filter by `?category=...` |
| `GET` | `/api/v1/profiles/me/skills` | Authenticated | List all skills assigned to the authenticated user's profile |
| `POST` | `/api/v1/profiles/me/skills` | Authenticated | Add skill to profile (`skillId`, `proficiencyLevel`) |
| `PUT` | `/api/v1/profiles/me/skills/{skillId}` | Authenticated | Update proficiency level of assigned skill |
| `DELETE` | `/api/v1/profiles/me/skills/{skillId}` | Authenticated | Remove skill from authenticated user's profile |
| `GET` | `/api/v1/profiles/me/education` | Authenticated | List education history for authenticated user |
| `POST` | `/api/v1/profiles/me/education` | Authenticated | Add education credential |
| `PUT` | `/api/v1/profiles/me/education/{id}` | Authenticated | Update education credential |
| `DELETE` | `/api/v1/profiles/me/education/{id}` | Authenticated | Delete education credential |
| `GET` | `/api/v1/profiles/me/experience` | Authenticated | List work experience history for authenticated user |
| `POST` | `/api/v1/profiles/me/experience` | Authenticated | Add work experience record |
| `PUT` | `/api/v1/profiles/me/experience/{id}` | Authenticated | Update work experience record |
| `DELETE` | `/api/v1/profiles/me/experience/{id}` | Authenticated | Delete work experience record |
| `GET` | `/api/v1/profiles/{username}` | Public | Get complete developer profile including skills, education, and work experience |

---

## 5. Frontend Architecture & Design System Integration

### 5.1 Architecture & Services
- **`src/types/profile.types.ts`**:
  - Extended domain interfaces: `Skill`, `ProfileSkill`, `AddSkillPayload`, `UpdateSkillProficiencyPayload`, `Education`, `EducationPayload`, `WorkExperience`, `ExperiencePayload`, `DeveloperProfile`.
- **`src/services/skills.service.ts`**:
  - `getSkillsCatalogue(query?, category?)`: Search master catalogue with query/category filters.
  - `getProfileSkills()`: Fetch profile skills.
  - `addSkillToProfile(payload)`: Assign skill.
  - `updateSkillProficiency(skillId, payload)`: Update level.
  - `removeSkillFromProfile(skillId)`: Delete skill.
- **`src/services/education.service.ts`**:
  - CRUD operations against `/api/v1/profiles/me/education`.
- **`src/services/experience.service.ts`**:
  - CRUD operations against `/api/v1/profiles/me/experience`.

### 5.2 UI Components & Views
- **`SkillsSection.tsx`**:
  - Displays skills grouped or filtered with category badges.
  - Visual proficiency indicators (`BEGINNER` to `EXPERT`).
  - Searchable catalogue selector with category filtering when adding skills.
  - Modal dialog for assigning skills and updating proficiency.
- **`EducationSection.tsx`**:
  - Structured academic timeline cards.
  - Clear formatting of degree, institution, field of study, and date ranges.
  - "Currently studying" badge handling with conditional end-date inputs.
  - Add and Edit modal dialog with validation and delete actions.
- **`ExperienceSection.tsx`**:
  - Work history cards with company name, job title, location, and employment type badges.
  - "Present" badge handling for active employment.
  - Add and Edit modal dialog with validation and delete actions.
- **Views**:
  - `/profile` (My Profile View): Shows real skills, experience, and education with direct "Add" and "Edit" action triggers.
  - `/profile/edit` (Profile Edit View): Dedicated tabs for Basic Information, Technical Skills, Work Experience, and Education History.
  - `/developers/[username]` (Public Showcase View): Clean tab navigation ("About & Skills", "Work Experience", "Education", "Projects") for discovering developers.

---

## 6. Verification and Test Results

### 6.1 Backend Test Suite
- **Executed Command:** `mvnw.cmd test`
- **Result:** `BUILD SUCCESS`
- **Summary:** `Tests run: 92, Failures: 0, Errors: 0, Skipped: 0`
- **Included Tests:**
  - `SkillServiceTest` (7 tests)
  - `EducationServiceTest` (6 tests)
  - `ExperienceServiceTest` (5 tests)
  - `SkillControllerIntegrationTest` (7 tests)
  - `EducationControllerIntegrationTest` (5 tests)
  - `ExperienceControllerIntegrationTest` (4 tests)
  - `DeveloperProfileControllerIntegrationTest` (6 tests)
  - `DeveloperProfileServiceTest` (6 tests)
  - `AuthControllerIntegrationTest` (14 tests)
  - `AuthServiceTest` (7 tests)
  - `TokenServiceTest` (6 tests)
  - `RegistrationServiceTest` (3 tests)
  - `JwtServiceTest` (4 tests)
  - `HealthControllerIntegrationTest` (2 tests)
  - `HealthServiceTest` (2 tests)
  - `BuildSyncApplicationTests` (1 test)

### 6.2 Frontend Code Quality & Build Verification
- **ESLint:** `npm run lint` exited `0` (0 errors, 0 warnings).
- **Next.js Production Build:** `npm run build --webpack` exited `0`.
  - All 16 routes compiled and validated:
    - `/` (Landing Page)
    - `/auth/login`
    - `/auth/register`
    - `/dashboard`
    - `/developers`
    - `/developers/[username]` (Dynamic developer profile view)
    - `/profile` (My profile view)
    - `/profile/edit` (Profile editor view)
    - `/projects`, `/find-teammates`, `/messages`, `/notifications`, `/settings`

### 6.3 End-to-End Live PostgreSQL Integration
- Executed against PostgreSQL 17 on `localhost:5432`:
  1. `POST /api/v1/auth/register` -> Created user `intuser_5fa97768`.
  2. `POST /api/v1/profiles` -> Created developer profile with headline "Senior Full-Stack Engineer".
  3. `POST /api/v1/profiles/me/skills` -> Added `Java` (EXPERT) to profile.
  4. `POST /api/v1/profiles/me/education` -> Added `Stanford University` (Master of Science).
  5. `POST /api/v1/profiles/me/experience` -> Added `Stripe` (Staff Software Engineer).
  6. `GET /api/v1/profiles/intuser_5fa97768` -> Successfully retrieved full profile with 1 skill, 1 education, 1 experience.
  7. `PUT /api/v1/profiles/me/skills/{skillId}` -> Updated proficiency to `INTERMEDIATE`.
  8. `DELETE /api/v1/profiles/me/experience/{id}` -> Deleted work experience record.
  9. `GET /api/v1/profiles/intuser_5fa97768` -> Verified final profile reflects 0 experience records, 1 skill, 1 education.
- **Status:** Complete verification passed with zero defects.

