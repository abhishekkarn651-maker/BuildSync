# BuildSync — Production Go-Live Checklist

This checklist defines the gate criteria required before moving BuildSync from staging to public production availability.

---

## 1. Automated Verification (Completed in Phase 8)

The following criteria have been verified via automated test suites, code analysis, and container build validations:

- [x] **Backend Regression:** All 239 backend test suites pass with 0 errors and 0 failures (`./mvnw clean test`).
- [x] **Backend Packaging:** Spring Boot executable archive builds cleanly (`backend-0.0.1-SNAPSHOT.jar`).
- [x] **Frontend Quality:** ESLint passes with 0 errors and 0 warnings (`npm run lint`).
- [x] **Frontend Production Build:** Next.js 16 Webpack build compiles all 18 routes (`npm run build`).
- [x] **Database Schema:** Flyway migrations `V1` through `V10` verified with clean baseline integrity.
- [x] **Redis Distributed Rate Limiter:** Real TCP-backed tests verify atomic token consumption, Lua script execution, shared counters across nodes, TTL key expiration, and fail-fast startup validation (`RedisProductionRateLimitTest`).
- [x] **In-Memory Rate Limiter:** Deterministic clock tests verify token bucket refills, burst caps, multi-cost tokens, and 1-hour inactivity TTL eviction.
- [x] **Client IP Anti-Spoofing:** Ignored `X-Forwarded-For` when `TRUSTED_PROXY_ENABLED=false` and safe leftmost client IP extraction when `true`.
- [x] **SHA-256 Storage Key Anonymization:** Raw client IPs and internal UUIDs are cryptographically hashed before interacting with cache storage.
- [x] **WebSocket & STOMP Authorization:** Frame interceptors drop unauthorized messages and typing events without database persistence or broadcast.
- [x] **Media Security:** Magic-byte validation, dimension boundaries, and 5MB limits enforced on avatar uploads.
- [x] **GitHub App Security:** OAuth PKCE validation, state token checks, and AES-256-GCM token encryption verified.
- [x] **Production Multi-Stage Dockerfiles:** Hardened containerfiles with non-root system users (`10001`) created for backend and frontend.
- [x] **Continuous Integration Pipeline:** Automated GitHub Actions workflow configured (`.github/workflows/ci.yml`).

---

## 2. Infrastructure & User-Provided Configuration (Required Prior to Launch)

The following items must be configured by the infrastructure team or hosting administrator:

- [ ] **Domain & DNS Provisioning:**
  - Allocate domain names: e.g. `buildsync.dev` (frontend) and `api.buildsync.dev` (backend).
  - Configure DNS A/CNAME records pointing to reverse proxy or cloud load balancers.
- [ ] **TLS / SSL Certificates:**
  - Issue trusted certificates (e.g. Let's Encrypt / AWS Certificate Manager).
  - Enable HTTP to HTTPS redirection (HSTS recommended).
- [ ] **Production Secrets Generation:**
  - Generate a 256-bit `JWT_SECRET` (`openssl rand -base64 48`).
  - Generate an AES-256-GCM `GITHUB_TOKEN_ENCRYPTION_KEY` (32 bytes).
  - Set production `DB_PASSWORD` and `REDIS_PASSWORD`.
- [ ] **PostgreSQL 17 Production Instance:**
  - Provision PostgreSQL 17 on AWS RDS / Google Cloud SQL / dedicated host.
  - Create dedicated non-superuser role `buildsync_app` with restricted privileges.
  - Verify automated daily snapshots and WAL archiving are active.
- [ ] **Redis 7 Production Instance:**
  - Provision Redis 7 cluster or managed instance with authentication password.
  - Verify persistent RDB/AOF storage enabled.
- [ ] **GitHub App Production Registration:**
  - Register production GitHub App in organization settings.
  - Update Authorization Callback URL to `https://api.buildsync.dev/api/v1/integrations/github/callback`.
  - Update Frontend Redirect URL to `https://buildsync.dev/settings/integrations/github`.
- [ ] **Reverse Proxy / Load Balancer:**
  - Ensure reverse proxy sets `X-Forwarded-For`, `X-Forwarded-Proto`, and `Host` headers.
  - Set `TRUSTED_PROXY_ENABLED=true` in backend environment.
  - Configure WebSocket upgrade headers (`Upgrade $http_upgrade`, `Connection "upgrade"`).

---

## 3. Post-Deployment Smoke Testing (Manual Verification)

Perform the following sanity checks on the deployed production instance:

- [ ] **System Health Check:**
  - Execute `curl -I https://api.buildsync.dev/api/v1/health` and verify `200 OK`.
  - Execute `curl -I https://buildsync.dev/` and verify `200 OK`.
- [ ] **User Registration & Login:**
  - Register a new account at `https://buildsync.dev/register`.
  - Log in and verify session persistence after browser tab refresh.
  - Inspect `buildsync-refresh` cookie in browser devtools: ensure `HttpOnly`, `Secure`, and `SameSite=Lax` are flagged.
- [ ] **Developer Profile & Media:**
  - Upload avatar image: verify image displays in UI and is served via `https://api.buildsync.dev/api/v1/media/avatars/*`.
  - Add skills, education, and experience: verify public profile `/developers/[username]` renders correctly.
- [ ] **Project Management & Collaboration:**
  - Create a public project and verify it appears on `/projects`.
  - Invite a colleague or secondary test account to collaborate.
  - Accept invitation and verify team member listing updates.
- [ ] **Real-Time STOMP Chat:**
  - Open `/messages` in two different browser windows under different accounts.
  - Send direct message: verify instant delivery via WebSocket without page refresh.
  - Verify typing indicator displays during active keystrokes.
- [ ] **GitHub Integration:**
  - Connect GitHub account via `/settings/integrations/github`.
  - Verify redirection to GitHub authorization page and return to BuildSync.
  - Trigger repository synchronization and showcase a public repository on profile.
- [ ] **Rate Limiting Verification:**
  - Trigger 11 rapid failed login requests: verify 11th request receives HTTP `429 Too Many Requests` with `Retry-After` header.
  - Verify legitimate requests succeed once the retry window expires.

