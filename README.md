# BuildSync

> **Bringing Developers and Projects Into Sync.**

BuildSync is a full-stack developer collaboration platform designed to connect engineers, showcase cutting-edge software projects, match teammates based on technical skills, manage project-scoped collaboration invitations, and provide real-time communication.

---

## 1. Project Overview & Features

### Core Capabilities
* **Developer Profiles & Skill Showcases:** Create verifiable profiles highlighting competencies, experience levels, and portfolio links.
* **Public Project Showcase:** Showcase software projects with technology tags, live demo links, and screenshot galleries.
* **Teammate Discovery Engine:** Filter developers and projects by technical stack, categories, and proficiency.
* **Collaboration Workflow:** Send, receive, and manage project-scoped invitations with atomic acceptance transactions.
* **In-App Notification Centre:** Real-time and persistent alerts for team invitations and project events.
* **Developer Dashboard:** Centralized workspace for tracking owned projects, team memberships, and invitation status.
* **Real-time Messaging:** Project-scoped rooms and direct messaging powered by STOMP over WebSocket.
* **GitHub Integration:** Display public repositories and sync developer activity directly with BuildSync projects.

---

## 2. Technology Stack

### Frontend
* **Framework:** Next.js (App Router)
* **Library:** React 19 / 18
* **Language:** TypeScript
* **Styling:** Tailwind CSS
* **Code Quality:** ESLint

### Backend
* **Language:** Java 17 LTS
* **Framework:** Spring Boot 3
* **Web & APIs:** Spring Web (REST under `/api/v1`)
* **Persistence & ORM:** Spring Data JPA / Hibernate 6
* **Database Migrations:** Flyway
* **API Documentation:** Springdoc OpenAPI / Swagger UI
* **Real-time Engine:** Spring WebSocket + STOMP
* **Build Tool:** Maven (via Maven Wrapper `mvnw`)

### Database & Storage
* **Relational Database:** PostgreSQL 17
* **Object Storage:** S3-compatible storage (for avatars and project screenshots)

---

## 3. Architecture Overview

BuildSync uses a **Decoupled Modular Monolith** architecture:
* **Decoupled Client:** The Next.js frontend is completely separated from the backend, communicating purely over secure REST (`/api/v1`) and WebSocket (`/ws`) protocols.
* **Domain Modules:** The Spring Boot backend organizes business boundaries (`auth`, `user`, `profile`, `skill`, `project`, `collaboration`, `notification`, `chat`, `health`) within a single deployable artifact, preventing distributed microservice overhead while enforcing domain separation.
* **Relational Integrity:** PostgreSQL enforces database-level constraints (foreign keys, partial unique indexes for single ownership and pending invitations, and check constraints).
* **Controlled Migrations:** Schema changes are version-controlled via Flyway, with Hibernate configured to validate (`ddl-auto: validate`) rather than auto-mutate the production schema.

For in-depth design details, refer to:
* [PROJECT_BLUEPRINT.md](docs/PROJECT_BLUEPRINT.md)
* [SYSTEM_ARCHITECTURE.md](docs/SYSTEM_ARCHITECTURE.md)
* [DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md)
* [DEVELOPMENT_ROADMAP.md](docs/DEVELOPMENT_ROADMAP.md)
* [RATE_LIMITING.md](docs/RATE_LIMITING.md)
* [PRODUCTION_DEPLOYMENT.md](docs/PRODUCTION_DEPLOYMENT.md)
* [GO_LIVE_CHECKLIST.md](docs/GO_LIVE_CHECKLIST.md)

---

## 4. Repository Structure

```text
BuildSync/
│
├── frontend/                     # Next.js App Router frontend application
│   ├── public/                   # Static assets
│   ├── src/
│   │   ├── app/                  # App Router pages and layouts
│   │   ├── components/           # Reusable UI components
│   │   ├── services/             # API client and backend service integrations
│   │   ├── hooks/                # Custom React hooks
│   │   ├── types/                # TypeScript type definitions
│   │   └── lib/                  # Utility helpers and configurations
│   ├── .env.example              # Frontend environment variables template
│   ├── package.json              # NPM dependencies and scripts
│   └── tsconfig.json             # TypeScript configuration
│
├── backend/                      # Spring Boot modular monolithic backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/buildsync/
│   │   │   │   ├── BuildSyncApplication.java
│   │   │   │   ├── common/       # Configuration, exceptions, responses
│   │   │   │   └── health/       # Health-check endpoints and DB verification
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/ # Flyway SQL migrations
│   │   └── test/                 # Integration and unit tests
│   ├── mvnw                      # Maven Wrapper (Linux / macOS)
│   ├── mvnw.cmd                  # Maven Wrapper (Windows)
│   └── pom.xml                   # Maven dependencies and build definition
│
├── database/
│   ├── migrations/               # Database migration scripts
│   ├── scripts/                  # Database setup scripts (01_init_database_and_user.sql)
│   └── documentation/            # Database ERD diagrams and specifications
│
├── docs/                         # Architecture, design, and roadmap documentation
│   ├── PROJECT_BLUEPRINT.md
│   ├── SYSTEM_ARCHITECTURE.md
│   ├── DATABASE_DESIGN.md
│   └── DEVELOPMENT_ROADMAP.md
│
├── .editorconfig                 # Coding style and indentation rules
├── .gitignore                    # Version control exclusions
└── README.md                     # Project documentation
```

---

## 5. Prerequisites & Environment Setup

Ensure the following tools are available on your system:
* **Node.js:** `v20+` or `v24+` (`node -v` and `npm -v`)
* **Java Development Kit:** Java 17 LTS (`javac -version`)
* **PostgreSQL:** Version 15+ (currently running PostgreSQL 17)
* **Git:** Version 2.x

---

## 6. PostgreSQL Database Configuration

1. Launch PostgreSQL and execute the initialization script located in `database/scripts/01_init_database_and_user.sql`:
   ```powershell
   psql -U postgres -f database/scripts/01_init_database_and_user.sql
   ```
2. This creates:
   - Database: `buildsync_db`
   - Dedicated User: `buildsync_app`
   - Default local dev password: `buildsync_dev_password`

3. Environment Variables for Backend:
   | Variable | Description | Default (Local Dev) |
   |---|---|---|
   | `DB_URL` | JDBC Connection String | `jdbc:postgresql://localhost:5432/buildsync_db` |
   | `DB_USERNAME` | Database username | `buildsync_app` |
   | `DB_PASSWORD` | Database password | `buildsync_dev_password` |
   | `SERVER_PORT` | Backend HTTP Port | `8080` |

---

## 7. Backend Startup Instructions

From the repository root:

```powershell
# Windows PowerShell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The Spring Boot backend will start on:
* **API Base:** `http://localhost:8080`
* **Health Check:** `http://localhost:8080/api/v1/health`
* **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
* **OpenAPI Specs:** `http://localhost:8080/v3/api-docs`

---

## 8. Frontend Startup Instructions

From the repository root:

```powershell
# Windows PowerShell
Set-Location frontend
npm install
npm run dev
```

The Next.js frontend will start on:
* **Web URL:** `http://localhost:3000`

### Frontend Environment Variables (`frontend/.env.local`)
Create a `.env.local` file inside `frontend/` (or copy from `.env.example`):
```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

---

## 9. Testing & Verification

### Running Frontend Verification
```powershell
cd frontend
npm run lint
npm run build
```

### Running Backend Verification
```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package
```

### Full Integration Verification
1. Start PostgreSQL server.
2. Start Spring Boot (`.\mvnw.cmd spring-boot:run`).
3. Start Next.js (`npm run dev`).
4. Navigate to `http://localhost:3000` to verify live health check connectivity to `http://localhost:8080/api/v1/health`.

---

## 10. Containerized Deployment (Docker Compose)

BuildSync includes production-ready multi-stage containerfiles and Docker Compose orchestration:

```bash
# Launch PostgreSQL 17, Redis 7, Spring Boot 3 Backend, and Next.js 16 Frontend
docker compose up -d

# Verify all services are healthy
docker compose ps
```

Services:
* **Frontend:** `http://localhost:3000`
* **Backend API & Health:** `http://localhost:8080/api/v1/health`
* **PostgreSQL 17:** `localhost:5432`
* **Redis 7:** `localhost:6379`

