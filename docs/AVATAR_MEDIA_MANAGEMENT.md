# BuildSync — Avatar & Media Management Documentation
**Phase 3, Part 4: Avatar & Media Management**  
**Technology Stack:** Java 17 LTS, Spring Boot 3.4.3, Spring Security 6, Spring Data JPA, PostgreSQL 17, Flyway 10, Next.js 16.4.3, React 19, TypeScript, Tailwind CSS 4.

---

## 1. Overview & Architectural Design

The Avatar & Media Management subsystem provides high-performance, secure, and pluggable asset storage and serving for developer profiles in BuildSync.

### Core Architectural Principles
1. **Decoupled Storage Abstraction (`StorageService`)**:
   - The application depends exclusively on the `StorageService` interface.
   - The default development implementation (`FileSystemStorageService`) saves files to a configurable directory on disk.
   - A cloud object-storage provider (e.g., AWS S3, Google Cloud Storage, Cloudflare R2) can be swapped in without modifying any profile or controller business logic.
2. **Metadata-Only Database Storage**:
   - Binary image bytes are never stored in PostgreSQL.
   - Only a controlled media reference path (e.g. `/api/v1/media/avatars/{filename}`) is saved in `developer_profiles.avatar_url`.
3. **Deep Content-Based Validation**:
   - Does not rely on client-supplied headers or file extensions alone.
   - Verifies magic byte signatures for JPEG (`FF D8 FF`) and PNG (`89 50 4E 47 0D 0A 1A 0A`).
   - Decodes image content via `ImageIO` to ensure visual stream integrity (rejects corrupted files, polyglots, and script injection).
   - Enforces dimension bounds (minimum 32x32 px, maximum 4096x4096 px) and size limit (maximum 5 MB).
4. **Path Traversal & Security Defenses**:
   - Generates collision-resistant unique filenames using UUIDs.
   - Rejects any filenames with directory traversal characters (`..`, `/`, `\`).
   - Enforces that resolved destination paths are descendants of the root avatar directory.
5. **Safe Replacement Order**:
   - On replacement, the new avatar is stored and persisted to the database first.
   - Old avatar files are deleted from storage only *after* database persistence succeeds.

---

## 2. Configuration & Environment Variables

| Variable | Default Value | Description |
|---|---|---|
| `STORAGE_UPLOAD_DIR` | `uploads` | Base local storage directory on the server filesystem |
| `STORAGE_AVATAR_SUBDIR` | `avatars` | Subdirectory designated for developer avatars |
| `spring.servlet.multipart.max-file-size` | `5MB` | Maximum allowed size per uploaded file |
| `spring.servlet.multipart.max-request-size` | `6MB` | Maximum total HTTP multipart request size |
| `buildsync.storage.max-file-size-bytes` | `5242880` | Programmatic byte limit check (5 MB) |
| `buildsync.storage.min-dimension` | `32` | Minimum allowed width & height in pixels |
| `buildsync.storage.max-dimension` | `4096` | Maximum allowed width & height in pixels |

```yaml
# application.yml excerpt
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 5MB
      max-request-size: 6MB

buildsync:
  storage:
    upload-dir: ${STORAGE_UPLOAD_DIR:uploads}
    avatar-dir: ${STORAGE_AVATAR_SUBDIR:avatars}
    max-file-size-bytes: 5242880
    min-dimension: 32
    max-dimension: 4096
```

---

## 3. Database Schema & Migration

### Migration File: `V5__add_avatar_url_to_developer_profiles.sql`
**Path:** `backend/src/main/resources/db/migration/V5__add_avatar_url_to_developer_profiles.sql`

```sql
-- ==============================================================================
-- BuildSync Phase 3 Part 4 - Avatar & Media Management Schema Migration
-- Adds avatar_url column to developer_profiles
-- ==============================================================================

ALTER TABLE developer_profiles
ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
```

- **Backward Compatibility**: `avatar_url` is nullable, ensuring existing profiles continue functioning seamlessly without an avatar.

---

## 4. REST API Specifications

### 4.1 Upload Avatar
- **Method & Path:** `POST /api/v1/profiles/me/avatar`
- **Security:** Bearer JWT required (`@AuthenticationPrincipal Jwt jwt`)
- **Content-Type:** `multipart/form-data`
- **Request Parameter:** `file` (Multipart file, JPEG or PNG, max 5 MB)
- **Responses:**
  - `200 OK`: Returns updated `ProfileResponse` including `avatarUrl`.
  - `400 Bad Request`: Empty file, corrupted image, unsupported format, or dimensions exceeded.
  - `401 Unauthorized`: Missing or invalid JWT access token.
  - `404 Not Found`: Developer profile does not exist for authenticated user.

### 4.2 Delete Avatar
- **Method & Path:** `DELETE /api/v1/profiles/me/avatar`
- **Security:** Bearer JWT required
- **Responses:**
  - `200 OK`: Returns updated `ProfileResponse` with `avatarUrl: null`.
  - `401 Unauthorized`: Missing or invalid JWT access token.
  - `404 Not Found`: Profile not found.

### 4.3 Public Media Streaming
- **Method & Path:** `GET /api/v1/media/avatars/{filename}`
- **Security:** Public (`permitAll()`)
- **Headers:**
  - `Content-Type`: `image/jpeg` or `image/png`
  - `Cache-Control`: `public, max-age=86400`
  - `Content-Disposition`: `inline; filename="..."`
- **Responses:**
  - `200 OK`: Binary image byte stream.
  - `404 Not Found`: File not found on disk or invalid filename.

---

## 5. Frontend Architecture & Design System Integration

### Components & Services
1. **`src/components/profile/AvatarUpload.tsx`**:
   - Reusable avatar management card with instant preview before saving.
   - Client-side pre-validation for file type (`image/jpeg`, `image/png`) and file size (`<= 5 MB`).
   - Spinner loading state during upload and deletion.
   - Remove photo confirmation modal.
   - Integrated into `/profile/edit` under the Basic Information tab.
2. **`src/components/ui/Avatar.tsx`**:
   - Renders image via Next.js `Image` component with fallback to initials.
   - Size presets: `xs`, `sm`, `md`, `lg`, `xl`.
   - Ring accents matching Cyber Ember Red (`#05`).
3. **`getAvatarUrl` Helper (`src/config/api.config.ts`)**:
   - Automatically prefixes relative backend paths (`/api/v1/media/...`) with the configured `NEXT_PUBLIC_API_BASE_URL`.
4. **Views Updated**:
   - `/profile`: Shows user avatar with a camera icon to trigger avatar editing.
   - `/profile/edit`: Dedicated Avatar Management card with interactive upload and remove controls.
   - `/developers/[username]`: Displays public developer's avatar with initials fallback.
   - `TopNavbar`: Displays user avatar in header menu chip.

---

## 6. Manual Testing Guide

### Prerequisites
- Backend running at `http://localhost:8080`.
- Frontend running at `http://localhost:3000`.

### Step 1: Register and Create Profile
```bash
# 1. Register User
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jane Doe","username":"janedoe","email":"jane@buildsync.dev","password":"Password123!"}'

# 2. Extract access token from response (export TOKEN="...")

# 3. Create Profile
curl -X POST http://localhost:8080/api/v1/profiles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"headline":"Full Stack Architect","bio":"Passionate builder","availabilityStatus":"AVAILABLE"}'
```

### Step 2: Upload Avatar
```bash
curl -X POST http://localhost:8080/api/v1/profiles/me/avatar \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@my-photo.jpg;type=image/jpeg"
```
**Expected Response:** `200 OK` with JSON containing `"avatarUrl": "/api/v1/media/avatars/<uuid>.jpg"`.

### Step 3: Stream Avatar Publicly
```bash
curl -I http://localhost:8080/api/v1/media/avatars/<uuid>.jpg
```
**Expected Response:** `HTTP/1.1 200 OK`, `Content-Type: image/jpeg`, `Cache-Control: max-age=86400, public`.

### Step 4: Delete Avatar
```bash
curl -X DELETE http://localhost:8080/api/v1/profiles/me/avatar \
  -H "Authorization: Bearer $TOKEN"
```
**Expected Response:** `200 OK` with `"avatarUrl": null`. Subsequent calls to `GET /api/v1/media/avatars/<uuid>.jpg` return `404 Not Found`.

