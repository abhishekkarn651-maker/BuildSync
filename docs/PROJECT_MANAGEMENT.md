# BUILDSYNC — PHASE 4: PROJECT MANAGEMENT SYSTEM

## 1. Overview & Architecture

The **Project Management System** serves as the core collaborative engine of BuildSync, connecting developers through shared software repositories, open-source initiatives, and collaborative project workspaces.

The system is architected around strict boundaries:
- Project creation, updates, and deletions are strictly governed by **authenticated user identity** resolved from JWT security contexts.
- Master technologies are linked to the verified master skills catalogue table (`skills`).
- Privacy is enforced at the database query level: private projects return `404 Not Found` to unauthenticated users or non-owners to eliminate project enumeration attack vectors.
- Fully integrated with the **Cyber Ember Red (`#05`)** design system across all pages and components.

---

## 2. Database Schema

Migration: `V7__create_project_management_tables.sql`

### 2.1 Table: `projects`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique project identifier |
| `owner_id` | UUID | NOT NULL, REFERENCES users(id) ON DELETE CASCADE | Project creator / owner |
| `title` | VARCHAR(120) | NOT NULL | Project title |
| `slug` | VARCHAR(160) | NOT NULL, UNIQUE | URL-safe slug derived from title & random suffix |
| `short_description`| VARCHAR(250) | NOT NULL | Brief summary for card display & previews |
| `detailed_description` | TEXT | NULL | Full project details, architecture & requirements |
| `category` | VARCHAR(50) | NOT NULL | Project category enum string |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'PLANNING' | Project status enum string |
| `visibility` | VARCHAR(20) | NOT NULL, DEFAULT 'PUBLIC' | Visibility enum string (`PUBLIC`, `PRIVATE`) |
| `github_url` | VARCHAR(255) | NULL | Optional GitHub repository link |
| `demo_url` | VARCHAR(255) | NULL | Optional live application / demo link |
| `start_date` | DATE | NULL | Project commencement date |
| `completion_date` | DATE | NULL | Project estimated/actual completion date |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Timestamp of creation |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Timestamp of last modification |

#### Constraints & Indexes
- Check constraint: `chk_projects_dates CHECK (completion_date IS NULL OR start_date IS NULL OR completion_date >= start_date)`
- Check constraint: `chk_projects_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'))`
- Check constraint: `chk_projects_status CHECK (status IN ('IDEA', 'PLANNING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'ARCHIVED'))`
- Check constraint: `chk_projects_category CHECK (category IN ('FULL_STACK', 'FRONTEND', 'BACKEND', 'MOBILE', 'AI_ML', 'DEVOPS', 'BLOCKCHAIN', 'CYBERSECURITY', 'OPEN_SOURCE', 'OTHER'))`
- Index `idx_projects_owner_id` ON `projects(owner_id)`
- Index `idx_projects_slug` ON `projects(slug)`
- Index `idx_projects_visibility` ON `projects(visibility)`
- Index `idx_projects_status` ON `projects(status)`
- Index `idx_projects_category` ON `projects(category)`
- Index `idx_projects_created_at` ON `projects(created_at DESC)`
- Index `idx_projects_updated_at` ON `projects(updated_at DESC)`

### 2.2 Table: `project_technologies`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique join record identifier |
| `project_id` | UUID | NOT NULL, REFERENCES projects(id) ON DELETE CASCADE | Associated project |
| `skill_id` | UUID | NOT NULL, REFERENCES skills(id) ON DELETE CASCADE | Associated master skill |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Association timestamp |

#### Constraints & Indexes
- Unique constraint: `uq_project_skill UNIQUE(project_id, skill_id)`
- Index `idx_project_tech_project_id` ON `project_technologies(project_id)`
- Index `idx_project_tech_skill_id` ON `project_technologies(skill_id)`

---

## 3. Domain Entities & Enums

### 3.1 Enums
- **`ProjectCategory`**: `FULL_STACK`, `FRONTEND`, `BACKEND`, `MOBILE`, `AI_ML`, `DEVOPS`, `BLOCKCHAIN`, `CYBERSECURITY`, `OPEN_SOURCE`, `OTHER`
- **`ProjectStatus`**: `IDEA`, `PLANNING`, `IN_PROGRESS`, `COMPLETED`, `ON_HOLD`, `ARCHIVED`
- **`ProjectVisibility`**: `PUBLIC`, `PRIVATE`

### 3.2 Domain Objects
- **`Project`**: Root aggregate holding ownership, metadata, URLs, timeline, and timestamps.
- **`ProjectTechnology`**: Unidirectional mapping linking `Project` to `Skill` from the master catalogue.

---

## 4. REST API Endpoints

Base Path: `/api/v1/projects`

### 4.1 Create Project
- **Endpoint:** `POST /api/v1/projects`
- **Auth:** Required (`ROLE_USER` or authenticated token)
- **Request Body (`CreateProjectRequest`):**
  ```json
  {
    "title": "BuildSync Platform",
    "shortDescription": "Developer collaboration and project showcase hub",
    "detailedDescription": "Deep dive into system architecture, microservices, and UI...",
    "category": "FULL_STACK",
    "status": "IN_PROGRESS",
    "visibility": "PUBLIC",
    "githubUrl": "https://github.com/BuildSync/platform",
    "demoUrl": "https://buildsync.dev",
    "startDate": "2026-01-15",
    "completionDate": "2026-06-30",
    "technologyIds": ["e5b4c10a-810a-4221-85b4-d57c793ba281"]
  }
  ```
- **Response:** `201 Created` with full `ProjectResponse`.

### 4.2 Browse Projects
- **Endpoint:** `GET /api/v1/projects`
- **Auth:** Public
- **Query Parameters:**
  - `keyword` (string): Searches title and detailedDescription case-insensitively
  - `category` (enum): Filters by `ProjectCategory`
  - `status` (enum): Filters by `ProjectStatus`
  - `technology` (string): Filters by skill name
  - `page` (int, default 0): 0-indexed page number
  - `size` (int, default 10): Items per page
  - `sort` (string, default `recently_updated`): `recently_updated`, `newest`, `title_asc`, `title_desc`
- **Response:** `200 OK` with Spring `Page<ProjectSummaryResponse>`. Only returns projects where `visibility = PUBLIC`.

### 4.3 My Projects
- **Endpoint:** `GET /api/v1/projects/mine`
- **Auth:** Required
- **Response:** `200 OK` with `List<ProjectResponse>` for all projects created by the calling user (both PUBLIC and PRIVATE).

### 4.4 Get Project by ID or Slug
- **Endpoint:** `GET /api/v1/projects/{identifier}`
- **Endpoint (Slug):** `GET /api/v1/projects/slug/{slug}`
- **Auth:** Optional / Conditional
- **Privacy Rules:**
  - If project is `PUBLIC`: Accessible by any visitor (anonymous or authenticated).
  - If project is `PRIVATE`: Accessible **only** if caller is authenticated and matches `project.owner.id`. Otherwise returns `404 Not Found`.

### 4.5 Update Project
- **Endpoint:** `PUT /api/v1/projects/{projectId}`
- **Auth:** Required (Owner only)
- **Rules:** If caller is not owner, returns `403 Forbidden`. If project does not exist, returns `404 Not Found`. Replaces technology associations atomically.
- **Response:** `200 OK` with updated `ProjectResponse`.

### 4.6 Delete Project
- **Endpoint:** `DELETE /api/v1/projects/{projectId}`
- **Auth:** Required (Owner only)
- **Rules:** If caller is not owner, returns `403 Forbidden`. Cascades deletion to `project_technologies`.
- **Response:** `204 No Content`.

### 4.7 Project Technologies
- **Endpoint:** `GET /api/v1/projects/{projectId}/technologies`
- **Auth:** Public (subject to project visibility check)
- **Response:** `200 OK` with `List<ProjectTechnologyResponse>`.

---

## 5. Security & Ownership Model

1. **Authentication Enforcement:** Write endpoints require valid `Authorization: Bearer <token>` headers.
2. **Owner Resolution:** The authenticated owner is never passed in client request bodies; it is extracted server-side using `userRepository.findByEmailOrUsername(principal)` from `SecurityContextHolder`.
3. **Anti-Enumeration Privacy:** Unauthenticated visitors and non-owners requesting private projects receive `404 Not Found` rather than `403 Forbidden`, preventing discovery of confidential repository names and slugs.
4. **Input Sanitization:** `@Valid`, `@NotBlank`, `@Size`, and cross-field date chronology checks (`@AssertTrue isDateRangeValid()`) guard against corrupted database records.

---

## 6. Frontend Implementation

### 6.1 Routes
- `/projects`: Dashboard & Discovery hub featuring debounced search (350ms), category selector, status selector, sort dropdown, Cyber Ember pulse skeletons, and "My Projects" tab.
- `/projects/create`: Interactive creation page with dynamic master skills selector, category dropdown, timeline date pickers, URL validation, and redirect to project details.
- `/projects/[projectId]`: High-fidelity project overview with owner profile card, technology pills, GitHub/Demo action buttons, and owner-only Edit / Delete confirmation modal.
- `/projects/[projectId]/edit`: Dedicated project edit page pre-filled with existing data and guarded by ownership verification.

### 6.2 Design System Compliance
- Primary accent: `#EF4444` (Cyber Ember Red).
- Surfaces: Deep obsidian cards with translucent borders (`border-border`, `hover:border-red-500/40`).
- Typography: High contrast `text-foreground` and readable `text-muted`.
- Full dual-theme fidelity across both dark and light modes.

