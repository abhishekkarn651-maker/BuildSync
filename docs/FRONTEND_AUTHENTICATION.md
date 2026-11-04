# BuildSync — Frontend Authentication Integration Guide
**Phase 2, Part 2: Frontend Authentication Integration**  
**Design System Identity:** Cyber Ember Red (`#05`)  
**Frontend Stack:** Next.js 16.4.3 (App Router), React 19, TypeScript, Tailwind CSS 4  
**Backend Stack:** Spring Boot 3.4.3, Spring Security 6, Nimbus OAuth2 Resource Server (HS256), PostgreSQL 17  

---

## 1. Overview & Architectural Principles

BuildSync implements a defense-in-depth, zero-trust token storage architecture between the Next.js frontend and the Spring Boot backend. 

### Core Security Principles
1. **Zero Access Token Persistence on Disk or Storage:**  
   The short-lived JWT access token (15-minute lifespan) is stored **strictly in JavaScript runtime memory** (`tokenStore`). It is **never** written to `localStorage`, `sessionStorage`, or accessible JavaScript cookies. This completely eliminates XSS token extraction vectors.
2. **HttpOnly Refresh Token Cookie:**  
   The long-lived refresh token (7 days standard, 30 days with "Remember Me") is stored exclusively inside an `HttpOnly`, `SameSite=Lax`, `Path=/api/v1/auth` cookie named `buildsync_refresh_token` set directly by Spring Boot. JavaScript running in the browser cannot read or modify this cookie.
3. **Silent Session Restoration on Boot:**  
   When the user opens BuildSync or reloads the tab, the frontend issues a silent session check (`POST /api/v1/auth/refresh`). If a valid refresh cookie exists, a fresh in-memory access token is issued, immediately restoring the session without prompting the user.
4. **Deduplicated Automatic 401 Retry:**  
   If an access token expires while a user is actively interacting with protected endpoints, the API client transparently pauses subsequent requests, executes a single-flight token refresh, and replays failed requests with the new access token.

```
┌────────────────────────────────────────────────────────┐
│                   Next.js Client                       │
│                                                        │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │      tokenStore       │   │     AuthContext      │  │
│  │ (In-Memory JWT Only)  │   │   (Session State)    │  │
│  └──────────┬────────────┘   └──────────┬───────────┘  │
│             │                           │              │
│             ▼                           ▼              │
│  ┌──────────────────────────────────────────────────┐  │
│  │              apiClient (Fetch Engine)            │  │
│  │  - Injects Authorization: Bearer <accessToken>   │  │
│  │  - Automatically includes credentials (cookies)  │  │
│  │  - Deduplicates concurrent 401 token refreshes   │  │
│  └──────────────────────────┬───────────────────────┘  │
└─────────────────────────────┼──────────────────────────┘
                              │
               HTTP / HTTPS   │ Credentials: Include
                              ▼
┌────────────────────────────────────────────────────────┐
│              Spring Boot Backend (:8080)               │
│                                                        │
│  POST /api/v1/auth/login    -> Set-Cookie & JWT Body   │
│  POST /api/v1/auth/register -> Set-Cookie & JWT Body   │
│  POST /api/v1/auth/refresh  -> Rotates Cookie & JWT    │
│  POST /api/v1/auth/logout   -> Revokes Cookie in DB    │
│  GET  /api/v1/auth/me       -> Validates Bearer Token  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Token Lifecycle & Storage Implementation

### 2.1 In-Memory Token Store (`src/services/token-store.ts`)
The `tokenStore` module holds the current JWT access token within a private module-level closure.

```typescript
let inMemoryAccessToken: string | null = null;

export const tokenStore = {
  getAccessToken(): string | null {
    return inMemoryAccessToken;
  },
  setAccessToken(token: string | null): void {
    inMemoryAccessToken = token;
  },
  clearAccessToken(): void {
    inMemoryAccessToken = null;
  },
};
```

### 2.2 Backend HttpOnly Cookie Contract
When `/api/v1/auth/login`, `/api/v1/auth/register`, or `/api/v1/auth/refresh` succeeds, Spring Boot sends the header:
```http
Set-Cookie: buildsync_refresh_token=<token_family_hash>; Path=/api/v1/auth; Max-Age=604800; HttpOnly; SameSite=Lax
```
When `/api/v1/auth/logout` is called, the backend revokes the refresh token record in PostgreSQL and expires the browser cookie:
```http
Set-Cookie: buildsync_refresh_token=; Path=/api/v1/auth; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax
```

---

## 3. Centralized Authentication Context (`src/context/AuthContext.tsx`)

The `AuthProvider` wraps the entire application tree in `src/app/layout.tsx`. It provides reactive authentication state and methods to all UI components.

### 3.1 State Machine
- `status`: `'loading'` | `'authenticated'` | `'unauthenticated'`
- `user`: `AuthUser | null`
- `isAuthenticated`: `boolean` (true when `status === 'authenticated' && !!user`)

### 3.2 Silent Session Restoration
On component mount, `AuthProvider` automatically invokes `refreshSession()`:
```typescript
const refreshSession = useCallback(async (): Promise<boolean> => {
  try {
    const response = await authService.refresh();
    if (response?.user) {
      setUser(response.user);
      setStatus('authenticated');
      return true;
    }
    setUser(null);
    tokenStore.clearAccessToken();
    setStatus('unauthenticated');
    return false;
  } catch {
    // 401 is benign on fresh unauthenticated visits
    setUser(null);
    tokenStore.clearAccessToken();
    setStatus('unauthenticated');
    return false;
  }
}, []);
```

---

## 4. API Client & Deduplicated 401 Token Refresh (`src/services/api-client.ts`)

Every HTTP request dispatched through `apiClient` automatically incorporates:
1. `credentials: 'include'` to pass backend cookies across `localhost:3000` and `localhost:8080`.
2. `Authorization: Bearer <token>` from `tokenStore`.

### 4.1 Single-Flight Shared Promise Deduplication
If multiple parallel API requests encounter an expired access token (HTTP 401), executing separate refresh requests could cause refresh-token family reuse detection failures in the backend. 

`apiClient` prevents this by sharing a single active refresh promise across all concurrent callers:

```typescript
let activeRefreshPromise: Promise<string | null> | null = null;

export async function performSilentRefresh(): Promise<string | null> {
  if (activeRefreshPromise) {
    return activeRefreshPromise; // Re-use pending refresh
  }

  activeRefreshPromise = (async () => {
    try {
      const response = await fetch(`${API_CONFIG.baseUrl}/api/v1/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        tokenStore.clearAccessToken();
        return null;
      }

      const data = await response.json();
      if (data?.accessToken) {
        tokenStore.setAccessToken(data.accessToken);
        return data.accessToken;
      }
      tokenStore.clearAccessToken();
      return null;
    } catch {
      tokenStore.clearAccessToken();
      return null;
    } finally {
      activeRefreshPromise = null;
    }
  })();

  return activeRefreshPromise;
}
```

---

## 5. Protected Route Guarding (`src/components/auth/ProtectedRoute.tsx`)

Protected routes are shielded on the client side:
- `/dashboard`
- `/collaborations`
- `/messages`
- `/notifications`
- `/settings`

Public routes remain accessible to guests:
- `/` (Landing)
- `/developers` & `/developers/[username]` (Discovery)
- `/projects` & `/projects/[projectId]` (Discovery)
- `/login` & `/register`

### 5.1 Route Guard Behavior
1. **Loading State:**  
   Renders a Cyber Ember Red circular glowing spinner:
   ```html
   <div className="w-12 h-12 rounded-full border-2 border-border border-t-[#EF4444] animate-spin" />
   ```
2. **Unauthenticated State:**  
   Redirects immediately to `/login?redirect=${encodeURIComponent(returnUrl)}`.
3. **Authenticated State:**  
   Renders the requested protected children.
4. **Already Authenticated on `/login` or `/register`:**  
   Automatically redirects back to `?redirect=` target or `/dashboard`.

---

## 6. End-to-End Verification Test Results

All 8 authentication lifecycle stages were validated against live backend instances (`localhost:8080`) and PostgreSQL 17:

| # | Test Scenario | Request Details | Observed HTTP Status | Result |
|---|---------------|-----------------|----------------------|--------|
| 1 | Register New User | `POST /api/v1/auth/register` | `201 Created` | **PASS** (Issued JWT + HttpOnly Cookie) |
| 2 | Access Protected `/me` | `GET /api/v1/auth/me` with Bearer | `200 OK` | **PASS** (Correct user identity returned) |
| 3 | Silent Token Refresh | `POST /api/v1/auth/refresh` via Cookie | `200 OK` | **PASS** (Access token rotated, new cookie set) |
| 4 | Access `/me` with Rotated Token | `GET /api/v1/auth/me` with new token | `200 OK` | **PASS** (Valid claims verified) |
| 5 | Logout | `POST /api/v1/auth/logout` | `200 OK` | **PASS** (Cookie expired, DB session revoked) |
| 6 | Refresh After Logout | `POST /api/v1/auth/refresh` | `401 Unauthorized` | **PASS** (Revoked cookie rejected) |
| 7 | Login with Credentials | `POST /api/v1/auth/login` | `200 OK` | **PASS** (Valid credentials authenticated) |
| 8 | Invalid Password Rejection | `POST /api/v1/auth/login` with bad pass | `401 Unauthorized` | **PASS** (Detailed error: "Invalid email or password") |

---

## 7. Next Steps (Phase 3)
- Phase 3, Part 1: Developer Profile backend services and GitHub account linking.
- Phase 3, Part 2: Developer Profile showcase and frontend editing interfaces.

