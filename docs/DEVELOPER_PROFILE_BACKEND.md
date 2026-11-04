# BuildSync — Developer Profile System Backend Documentation
**Phase 3, Part 1: Database Design & Backend APIs**  
**Technology Stack:** Java 17 LTS, Spring Boot 3.4.3, Spring Security 6, Spring Data JPA, PostgreSQL 17, Flyway 10, OpenAPI / Swagger  

---

## 1. Overview & Architectural Principles

The Developer Profile System establishes the core professional identity foundation of BuildSync. It enables authenticated developers to showcase their experience, technical headline, biographical details, location, social/code repository links, and availability for project collaboration.

### Strict Architectural Principles
1. **Re-use Existing Users Identity:**  
   Profiles directly reference the primary `users` table via foreign key (`user_id`). No duplicate user records or shadow credentials are created.
2. **Strict Profile Ownership:**  
   Mutations (`POST /api/v1/profiles`, `PUT /api/v1/profiles/me`) and authenticated inspection (`GET /api/v1/profiles/me`) resolve the user identity solely from cryptographically signed, validated JWT access tokens (`jwt.getSubject()`). Users cannot modify or tamper with other developers' profiles.
3. **1-to-1 Duplicate Protection:**  
   A unique constraint on `developer_profiles(user_id)` ensures each developer account corresponds to exactly one profile. Duplicate creation requests return `409 Conflict`.
4. **Zero Token or Credential Storage:**  
   Profiles never store passwords, hashes, refresh tokens, or JWTs.
5. **Public Showcase Discovery:**  
   The `GET /api/v1/profiles/{username}` endpoint is open for public developer discovery, matching BuildSync's collaboration and project matching mission.

---

## 2. Database Schema & Flyway Migration

### Migration: `V3__create_developer_profile_tables.sql`
* **File:** `src/main/resources/db/migration/V3__create_developer_profile_tables.sql`
* **Target Table:** `developer_profiles`

```sql
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
```

---

## 3. Domain Model & Enums

### 3.1 `DeveloperProfile` (`com.buildsync.profile.domain`)
* Unidirectional `@OneToOne` association to `User`:
  ```java
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;
  ```
* Automatic timestamp management via `@PrePersist` and `@PreUpdate`.

### 3.2 Domain Enums
* **`ExperienceLevel`**: `JUNIOR`, `MID_LEVEL`, `SENIOR`, `LEAD`
* **`AvailabilityStatus`**: `AVAILABLE`, `BUSY`, `MENTORING`, `NOT_AVAILABLE`

---

## 4. API Specification

All developer profile endpoints are exposed under `/api/v1/profiles`.

### 4.1 Create Developer Profile
* **Endpoint:** `POST /api/v1/profiles`
* **Security:** Authenticated (`Bearer <accessToken>`)
* **Request Body (`CreateProfileRequest`):**
  ```json
  {
    "headline": "Lead Systems Architect",
    "bio": "Designing resilient microservices and distributed databases.",
    "about": "Full-stack developer with extensive experience in Spring Boot, Next.js, and PostgreSQL.",
    "location": "San Francisco, CA",
    "websiteUrl": "https://profiletest.dev",
    "githubUrl": "https://github.com/bharatkumar",
    "linkedinUrl": "https://linkedin.com/in/bharatkumar",
    "twitterUrl": "https://x.com/bharatkumar",
    "experienceLevel": "SENIOR",
    "yearsOfExperience": 7,
    "availabilityStatus": "AVAILABLE"
  }
  ```
* **Response Status:** `201 Created`
* **Response Body (`ProfileResponse`):**
  ```json
  {
    "id": "0f11be0e-78bf-476b-b78b-9e70df49768c",
    "userId": "4fed136a-c8d1-4e2d-a215-df7e91405b45",
    "username": "bharatkumar",
    "fullName": "Bharat Kumar",
    "email": "bharat@buildsync.dev",
    "headline": "Lead Systems Architect",
    "bio": "Designing resilient microservices and distributed databases.",
    "about": "Full-stack developer with extensive experience in Spring Boot, Next.js, and PostgreSQL.",
    "location": "San Francisco, CA",
    "websiteUrl": "https://profiletest.dev",
    "githubUrl": "https://github.com/bharatkumar",
    "linkedinUrl": "https://linkedin.com/in/bharatkumar",
    "twitterUrl": "https://x.com/bharatkumar",
    "experienceLevel": "SENIOR",
    "yearsOfExperience": 7,
    "availabilityStatus": "AVAILABLE",
    "createdAt": "2026-11-01T19:08:27.665487Z",
    "updatedAt": "2026-11-01T19:08:27.665487Z"
  }
  ```
* **Error Responses:**
  * `400 Bad Request` — Validation failure (e.g., malformed URL, invalid years of experience).
  * `401 Unauthorized` — Missing or expired JWT access token.
  * `409 Conflict` — Developer profile already exists for the authenticated user.

---

### 4.2 Get Current Developer Profile
* **Endpoint:** `GET /api/v1/profiles/me`
* **Security:** Authenticated (`Bearer <accessToken>`)
* **Response Status:** `200 OK`
* **Error Responses:**
  * `401 Unauthorized` — Missing or expired token.
  * `404 Not Found` — Profile has not been created yet for current user.

---

### 4.3 Update Current Developer Profile
* **Endpoint:** `PUT /api/v1/profiles/me`
* **Security:** Authenticated (`Bearer <accessToken>`)
* **Request Body (`UpdateProfileRequest`):** (Full or partial update of profile attributes)
* **Response Status:** `200 OK`
* **Error Responses:**
  * `400 Bad Request` — Validation failure.
  * `401 Unauthorized` — Missing or expired token.
  * `404 Not Found` — Profile does not exist.

---

### 4.4 Get Developer Profile by Username
* **Endpoint:** `GET /api/v1/profiles/{username}`
* **Security:** Public (`permitAll`)
* **Description:** Case-insensitive lookup of a developer's public profile by their unique username.
* **Response Status:** `200 OK`
* **Error Responses:**
  * `404 Not Found` — No user or profile exists for the given username.

---

## 5. Security Configuration

In `SecurityConfig.java`:
```java
// Developer Profiles
.requestMatchers(HttpMethod.GET, "/api/v1/profiles/me").authenticated()
.requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me").authenticated()
.requestMatchers(HttpMethod.POST, "/api/v1/profiles").authenticated()
.requestMatchers(HttpMethod.GET, "/api/v1/profiles/**").permitAll()
```
The specific `/me` path matches before the wildcard `/**`, ensuring authenticated endpoints are strictly guarded while public profile lookups remain accessible without authentication.

---

## 6. Verification and Test Results

### 6.1 Unit & Integration Test Suites
Executed via Maven Surefire:
* `DeveloperProfileServiceTest`: 10/10 passed (creation, validation, duplicate prevention, updates, lookups).
* `DeveloperProfileControllerIntegrationTest`: 11/11 passed (Spring Security guards, MockMvc HTTP contracts, error responses).
* `AuthControllerIntegrationTest`: 14/14 passed (registration, login, cookie rotation, logout).
* `AuthServiceTest` & `RegistrationServiceTest`: 11/11 passed.
* `TokenServiceTest` & `JwtServiceTest`: 9/9 passed.
* `HealthControllerIntegrationTest` & `HealthServiceTest`: 3/3 passed.
* `BuildSyncApplicationTests`: 1/1 passed.

**Total Tests Run: 59 | Failures: 0 | Errors: 0 | Skipped: 0**

### 6.2 Flyway Schema Migration Verification
* Current schema version: `v3`
* Migration: `V3__create_developer_profile_tables.sql` applied successfully to PostgreSQL 17 in `00:00.083s`.

### 6.3 Live End-to-End API Verification
All 10 live test scenarios passed with 100% success against running Spring Boot and PostgreSQL instances:
1. Registration of new developer account (`POST /api/v1/auth/register`) -> `201 Created`
2. `GET /api/v1/profiles/me` prior to creation -> `404 Not Found`
3. `POST /api/v1/profiles` unauthenticated -> `401 Unauthorized`
4. `POST /api/v1/profiles` authenticated -> `201 Created`
5. Duplicate `POST /api/v1/profiles` -> `409 Conflict`
6. `GET /api/v1/profiles/me` authenticated -> `200 OK`
7. `PUT /api/v1/profiles/me` authenticated -> `200 OK`
8. Public `GET /api/v1/profiles/{username}` -> `200 OK`
9. Public `GET /api/v1/profiles/{nonexistent}` -> `404 Not Found`
10. Existing auth regression (`POST /login`, `POST /refresh`, `POST /logout`) -> All `200 OK`

