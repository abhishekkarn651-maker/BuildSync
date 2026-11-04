# BuildSync — Project Blueprint

**Tagline:** Bringing Developers and Projects Into Sync.  
**Category:** Full-Stack Developer Collaboration Platform  
**Target Architecture:** Modular Monolith with Decoupled Next.js Frontend  

---

## 1. Executive Summary & Product Boundary

BuildSync is designed to solve a fundamental challenge faced by developers, students, and engineers: finding complementary technical collaborators and showcasing software projects effectively. While platforms like GitHub excel at source-code hosting and version control, they are not tailored for team formation, skill-based matchmaking, or active project-scoped recruitment.

BuildSync bridges this gap:
- **BuildSync is:** A professional developer showcase, teammate discovery engine, project-scoped collaboration hub, and real-time workspace communicator.
- **BuildSync is NOT:** A source-code hosting service or git repository manager. GitHub remains the source of truth for code; BuildSync links to repositories and highlights architecture, technologies, and team roles.

---

## 2. Core V1 Feature Set

1. **Developer Profiles & Skill Showcases:**
   - Detailed developer identity: bio, portfolio links, education, experience levels.
   - Standardized skill catalogue (Languages, Frameworks, Databases, Tools, Cloud).
   - Verifiable GitHub account linkage to display public repositories.

2. **Public Project Showcase:**
   - Rich project presentation: title, slug, category, technology stack, media/screenshots.
   - Clear project status (e.g., `PLANNING`, `IN_PROGRESS`, `LOOKING_FOR_CONTRIBUTORS`, `COMPLETED`).
   - Links to live demos and GitHub repositories.

3. **Developer & Teammate Discovery:**
   - Multi-criteria filtering by skills, proficiency, and experience level.
   - Text search across developer profiles and project descriptions.

4. **Project-Scoped Collaboration Workflow:**
   - Owners can send structured collaboration invitations to developers.
   - Recipients can review, accept, or reject invitations.
   - Team membership tracking with explicit role definitions (`OWNER`, `MEMBER`).

5. **Notification Centre:**
   - Real-time and persistent in-app notifications for invitations, responses, and project updates.

6. **Developer Dashboard:**
   - Centralized management view of owned projects, joined teams, and active collaboration requests.

7. **Project & Direct Messaging (Milestone 5):**
   - Project-scoped group rooms created automatically for each project.
   - Direct messaging between verified collaborators.
   - Real-time updates powered by WebSocket & STOMP.

---

## 3. Technology Stack Reference

| Layer | Approved Technology | Key Responsibilities |
|---|---|---|
| **Frontend** | Next.js (React, TypeScript, Tailwind CSS) | Responsive UI, SSR/SSG for public profiles/showcases, interactive client dashboard. |
| **Backend** | Java 17 LTS, Spring Boot 3 | Modular monolithic REST API under `/api/v1`, validation, domain security. |
| **Database** | PostgreSQL 17 | Relational persistence, strict referential integrity, full-text search. |
| **Migrations**| Flyway | Version-controlled, repeatable, automated database migrations. |
| **Security** | Spring Security, JWT & HttpOnly Refresh Tokens | Stateless authorization with secure session rotation. |
| **Real-time** | Spring WebSocket + STOMP | Project chat rooms and live collaboration notifications. |
| **Docs** | OpenAPI 3.0 / Swagger UI | Interactive, live backend API documentation. |

---

## 4. Acceptance Criteria for MVP Release

1. User can register, authenticate securely, and maintain their developer profile with skills.
2. User can showcase projects with technology tags and view other public projects.
3. User can search and filter developers by skill tags.
4. Project owners can invite developers; invitees can accept/reject; membership updates atomically.
5. In-app notifications accurately reflect invitation lifecycles.
6. The dashboard aggregates projects and active requests.
