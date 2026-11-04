# BuildSync — Frontend Developer Profile System Documentation
**Phase 3, Part 2: Frontend Developer Profile Creation & Editing**  
**Design System Identity:** Cyber Ember Red (`#05`)  
**Frontend Stack:** Next.js 16.4.3 (App Router), React 19, TypeScript, Tailwind CSS 4, Lucide React  
**Backend Stack:** Spring Boot 3.4.3, Spring Security 6, Spring Data JPA, PostgreSQL 17  

---

## 1. Overview & Architecture

Phase 3, Part 2 connects the BuildSync frontend with the Spring Boot profile APIs implemented in Phase 3, Part 1. It enables registered developers to create, showcase, and edit their professional developer identity, while eliminating hardcoded demo identities (such as Bharat Kumar) from representing authenticated user sessions.

### Architecture Highlights
* **Service-Centric API Client:** All profile operations flow through [`src/services/profile.service.ts`](file:///d:/BUILDSYNC/frontend/src/services/profile.service.ts), utilizing the existing authenticated `apiClient` with automatic Bearer token injection and deduplicated 401 token refresh.
* **Strong Typing:** Strict TypeScript contracts defined in [`src/types/profile.types.ts`](file:///d:/BUILDSYNC/frontend/src/types/profile.types.ts) mirror the backend `DeveloperProfile`, `CreateProfileRequest`, `UpdateProfileRequest`, `ExperienceLevel`, and `AvailabilityStatus`.
* **Zero Client User-ID Reliance:** Neither profile creation nor editing passes a client-provided `user_id`. The backend resolves identity strictly from the verified JWT access token subject.

---

## 2. Implemented Pages & Features

### 2.1 Profile Setup (`/profile/setup`)
* **File:** [`src/app/profile/setup/page.tsx`](file:///d:/BUILDSYNC/frontend/src/app/profile/setup/page.tsx)
* **Access:** Authenticated (`AppLayout requireAuth`)
* **Functionality:**
  * Checks if the user already has a profile; if so, displays options to view or edit their profile.
  * Form grouped into 3 sections:
    1. **Professional Identity:** Professional Headline (0/150 chars), Short Bio (0/500 chars), and Detailed About Me.
    2. **Experience & Availability:** Experience Level dropdown (`JUNIOR`, `MID_LEVEL`, `SENIOR`, `LEAD`), Years of Experience (0–100), Availability Status dropdown (`AVAILABLE`, `BUSY`, `MENTORING`, `NOT_AVAILABLE`), and Location (max 100).
    3. **Online Presence:** Portfolio / Website URL, GitHub URL, LinkedIn URL, and Twitter/X URL.
  * Validation: Client-level URL protocol verification (`http://` or `https://`) and numeric bounds.
  * Dispatches `POST /api/v1/profiles`.
  * Redirects to `/profile` on success.

### 2.2 My Profile (`/profile`)
* **File:** [`src/app/profile/page.tsx`](file:///d:/BUILDSYNC/frontend/src/app/profile/page.tsx)
* **Access:** Authenticated (`AppLayout requireAuth`)
* **Functionality:**
  * Dispatches `GET /api/v1/profiles/me`.
  * **Missing Profile State:** If the backend returns `404 Not Found`, renders a Cyber Ember Red `EmptyState` prompting the user to "Complete Your Developer Profile" with a direct button to `/profile/setup`.
  * **Active Profile State:** Displays the user's real name, username, availability badge, headline, bio, detailed background, location, experience metrics, and verified repository links.
  * Provides quick actions: "Edit Profile" (`/profile/edit`), "Public View" (`/developers/${username}`), and "Share" (copies public profile link to clipboard).

### 2.3 Profile Editing (`/profile/edit`)
* **File:** [`src/app/profile/edit/page.tsx`](file:///d:/BUILDSYNC/frontend/src/app/profile/edit/page.tsx)
* **Access:** Authenticated (`AppLayout requireAuth`)
* **Functionality:**
  * Fetches current profile via `GET /api/v1/profiles/me` and prepopulates all fields.
  * If no profile exists yet, redirects immediately to `/profile/setup`.
  * Allows updating headline, bio, about, location, experience level, years of experience, availability status, and social/code links.
  * Dispatches `PUT /api/v1/profiles/me`.
  * Displays inline loading states, error alerts, and success confirmations.

### 2.4 Public Showcase (`/developers/[username]`)
* **File:** [`src/app/developers/[username]/page.tsx`](file:///d:/BUILDSYNC/frontend/src/app/developers/[username]/page.tsx)
* **Access:** Public (`permitAll`)
* **Functionality:**
  * Fetches developer details via `profileService.getPublicProfile(username)`.
  * If found in PostgreSQL, displays live database profile information.
  * If viewing user is the profile owner (`user.username === username`), surfaces an "Edit My Profile" button.
  * Fallback to mock developers for exploratory discovery cards; displays `EmptyState` ("Developer Not Found") if non-existent.

### 2.5 Dashboard Personalization & Onboarding Banner (`/dashboard`)
* **File:** [`src/app/dashboard/page.tsx`](file:///d:/BUILDSYNC/frontend/src/app/dashboard/page.tsx)
* **Functionality:**
  * Displays personalized greeting: `"Welcome back, [Actual User Name]!"`.
  * Asynchronously checks profile status:
    * If no profile is found: Displays a prominent Cyber Ember Red action banner: `"Complete Your Developer Profile — Showcase your skills, introduce yourself, and help other developers discover you."` with a primary `"Complete Profile"` button.
    * If profile is active: Displays a confirmation banner showing the active headline and quick links to view or edit the profile.

### 2.6 Navigation Updates
* **Sidebar (`src/components/layout/Sidebar.tsx`):** "Profile" item now links directly to the authenticated user's profile (`/profile`) instead of the hardcoded Bharat Kumar link.
* **TopNavbar (`src/components/layout/TopNavbar.tsx`):** Dropdown menu includes "My Profile" (`/profile`) and "Edit Profile" (`/profile/edit`).

---

## 3. Verification & Build Results

1. **ESLint:**
   * Executed `npm run lint`.
   * Result: **0 errors, 0 warnings**.
2. **Next.js Production Build:**
   * Executed `npm run build --webpack`.
   * Compiled all 16 static and dynamic routes cleanly:
     * `/` (Static)
     * `/collaborations` (Static)
     * `/dashboard` (Static)
     * `/developers` (Static)
     * `/developers/[username]` (Dynamic)
     * `/login` (Static)
     * `/messages` (Static)
     * `/notifications` (Static)
     * `/profile` (Static)
     * `/profile/edit` (Static)
     * `/profile/setup` (Static)
     * `/projects` (Static)
     * `/projects/[projectId]` (Dynamic)
     * `/register` (Static)
     * `/settings` (Static)
3. **Live Route Health Verification:**
   * `http://localhost:8080/api/v1/health` -> `200 OK`
   * `http://localhost:3000/` -> `200 OK`
   * `http://localhost:3000/dashboard` -> `200 OK`
   * `http://localhost:3000/profile` -> `200 OK`
   * `http://localhost:3000/profile/setup` -> `200 OK`
   * `http://localhost:3000/profile/edit` -> `200 OK`
   * `http://localhost:3000/developers` -> `200 OK`
   * `http://localhost:3000/developers/bharatkumar` -> `200 OK`

