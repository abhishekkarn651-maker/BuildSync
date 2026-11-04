# BuildSync — Rate Limiting & Security Hardening Architecture

## 1. Overview & Architecture

BuildSync implements a production-grade, centralized rate-limiting architecture designed to safeguard REST endpoints, authentication services, media uploads, and real-time STOMP messaging against brute-force attacks, resource exhaustion, and denial-of-service attempts.

The rate limiting subsystem is designed around a pluggable SPI (`RateLimitStorage`) supporting two interchangeable storage engines:

```
                                 [ Incoming Request ]
                                          │
                                          ▼
                         [ ClientIpResolver / JWT Principal ]
                                          │
                                          ▼
                                [ RateLimitFilter ]
                                          │
                                          ▼
                                [ RateLimitService ]
                                          │
                     ┌────────────────────┴────────────────────┐
                     ▼                                         ▼
         [ InMemoryRateLimitStorage ]              [ RedisRateLimitStorage ]
         - Token Bucket Algorithm                  - Atomic Lua Script
         - Thread-Safe (ConcurrentMap)             - Distributed Shared State
         - Inactive Bucket TTL Eviction            - Automatic Redis Key TTL
         - Injectable Clock for Testing            - Fail-Fast Startup Validation
         - Zero External Dependencies              - Clustered Production Ready
```

### Pluggable Storage Engines

1. **In-Memory Storage (`InMemoryRateLimitStorage`)**:
   - Engine: Thread-safe token bucket algorithm with fine-grained per-bucket locks.
   - Lifecycle: Automatically manages memory with an eviction threshold (`50,000` buckets) and a 1-hour inactivity TTL.
   - Testability: Accepts an injectable `java.time.Clock`, enabling instant deterministic time advancement in unit and integration tests without thread sleeps.
   - Use Cases: Local development, continuous integration, and lightweight deployments with zero Redis dependency.

2. **Distributed Redis Storage (`RedisRateLimitStorage`)**:
   - Engine: Atomic Lua script executing token bucket calculations in a single Redis round-trip.
   - Consistency: Distributed shared state across all containerized backend replicas.
   - Key Management: Automatic key expiration (`EXPIRE`) configured to twice the refill period (minimum 1 hour).
   - Fail-Fast Protection: In `redis` mode (`buildsync.rate-limit.storage=redis`), the application validates Redis connectivity on startup (`PING`). If Redis is unreachable, the application halts immediately with an `IllegalStateException` rather than failing silently.

---

## 2. Configured Rate Limiting Policies

BuildSync enforces 14 granular rate-limiting policies across all public and protected surfaces:

| Policy | Target Endpoint / Action | Identifier Scope | Capacity | Refill Rate | Window / Period |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **`AUTH_LOGIN`** | `POST /api/v1/auth/login` | Client IP | 10 req | 10 req | 300s (5 min) |
| **`AUTH_REGISTER`** | `POST /api/v1/auth/register` | Client IP | 5 req | 5 req | 3600s (1 hour) |
| **`AUTH_REFRESH`** | `POST /api/v1/auth/refresh` | User UUID + Client IP | 30 req | 30 req | 900s (15 min) |
| **`GENERAL_AUTHENTICATED`** | General protected REST APIs | User UUID | 120 req | 120 req | 60s (1 min) |
| **`GENERAL_ANONYMOUS`** | General public REST APIs | Client IP | 60 req | 60 req | 60s (1 min) |
| **`SEARCH_DEVELOPERS`** | `GET /api/v1/profiles` | Client IP / User | 60 req | 60 req | 60s (1 min) |
| **`SEARCH_PROJECTS`** | `GET /api/v1/projects` | Client IP / User | 60 req | 60 req | 60s (1 min) |
| **`AVATAR_UPLOAD`** | `POST /api/v1/profiles/me/avatar` | User UUID | 10 req | 10 req | 3600s (1 hour) |
| **`COLLABORATION_ACTIONS`** | `/api/v1/collaborations/*`, invitations, join requests | User UUID | 20 req | 20 req | 60s (1 min) |
| **`DIRECT_MESSAGING`** | STOMP `/app/conversations/{id}/send` | User UUID | 30 msg | 30 msg | 60s (1 min) |
| **`PROJECT_GROUP_MESSAGING`**| STOMP `/app/conversations/{id}/send` | User UUID | 30 msg | 30 msg | 60s (1 min) |
| **`WEBSOCKET_CONNECT`** | WebSocket Handshake `/ws` | Client IP | 10 attempts | 10 attempts | 60s (1 min) |
| **`TYPING_EVENTS`** | STOMP `/app/conversations/{id}/typing` | User UUID | 60 events | 60 events | 60s (1 min) |
| **`GITHUB_SYNC`** | `POST /api/v1/integrations/github/sync` | User UUID | 5 req | 5 req | 3600s (1 hour) |

---

## 3. Safe Client Identity & IP Resolution

### Strict Anti-Spoofing Rules
To prevent attackers from bypassing rate limits using spoofed proxy headers, BuildSync employs `ClientIpResolver`:

- **Untrusted Proxy Mode (`TRUSTED_PROXY_ENABLED=false`, Default):**
  - All `X-Forwarded-For`, `X-Real-IP`, and similar headers are **completely ignored**.
  - The client identity is strictly determined from the socket remote address (`request.getRemoteAddr()`).
- **Trusted Proxy Mode (`TRUSTED_PROXY_ENABLED=true`):**
  - Enabled only when operating behind a verified reverse proxy (e.g., NGINX, Cloudflare, AWS ALB).
  - Inspects `X-Forwarded-For` and securely parses the leftmost client IP address, trimming whitespace and rejecting invalid or `"unknown"` tokens.

### Authenticated Request Resolution
- Authenticated requests are always keyed by the verified User UUID (`SecurityContextHolder.getContext().getAuthentication().getName()`).
- Request body parameters or query string user IDs are **never** trusted for rate-limiting calculations.

### Anonymized Storage Keys
- To maintain security and avoid leaking sensitive IP addresses or internal user identifiers, all storage keys are salted with the policy name and hashed using **SHA-256**:
  ```
  rl:{policy}:{sha256(raw_identifier)}
  ```
- Neither raw IP addresses nor internal storage keys are ever reflected in response headers or error bodies.

---

## 4. HTTP 429 Response Format & Standard Headers

### Quota Headers on Permitted Responses
Every HTTP response evaluated by the rate-limiting filter contains RFC-compliant tracking headers:
```http
RateLimit-Limit: 120
RateLimit-Remaining: 119
RateLimit-Reset: 58
```

### Rate Limit Exceeded Response (HTTP 429)
When a client exhausts their allocated quota:
1. HTTP Status is set to **`429 Too Many Requests`**.
2. Standard headers are attached:
   ```http
   Retry-After: 30
   RateLimit-Limit: 120
   RateLimit-Remaining: 0
   RateLimit-Reset: 30
   ```
3. A JSON error body adhering to `GlobalExceptionHandler` and `ErrorResponse` is returned:
   ```json
   {
     "timestamp": "2026-11-03T20:45:00",
     "status": 429,
     "error": "TOO_MANY_REQUESTS",
     "message": "Too many requests. Please try again later.",
     "path": "/api/v1/auth/login"
   }
   ```
4. Filter execution terminates immediately without passing the request to downstream controllers.

### Whitelisted Endpoints
The following endpoints bypass rate limiting to guarantee service availability and diagnostic access:
- `/api/v1/health`
- `/actuator/**`
- HTTP `OPTIONS` requests (CORS preflight)
- Swagger / OpenAPI documentation (`/swagger-ui/**`, `/v3/api-docs/**`)

---

## 5. WebSocket & STOMP Messaging Protection

Real-time communication is protected at both transport and application protocol levels:

### Transport Layer: `RateLimitHandshakeInterceptor`
- Intercepts incoming WebSocket HTTP upgrade requests at `/ws`.
- Evaluates the `WEBSOCKET_CONNECT` policy (10 connection attempts / minute / IP).
- If rejected, responds with HTTP status 429 and aborts the handshake before any WebSocket session or broker resources are allocated.

### Application Layer: `StompAuthChannelInterceptor`
- Intercepts inbound STOMP frames in `preSend`:
  - **`SEND` to `/app/conversations/{id}/send`**: Validates user membership and enforces `DIRECT_MESSAGING` / `PROJECT_GROUP_MESSAGING` (30 messages / min / user).
  - **`SEND` to `/app/conversations/{id}/typing`**: Enforces `TYPING_EVENTS` (60 events / min / user).
- **Enforcement Semantics**: When a limit is exceeded, a `RateLimitExceededException` is thrown. The frame is dropped immediately and is **never persisted in PostgreSQL** or broadcast to subscribers.

---

## 6. Frontend Client Integration

In `frontend/src/services/api-client.ts`, the frontend HTTP client has been upgraded with a specialized `RateLimitError`:

```typescript
export class RateLimitError extends ApiError {
  constructor(
    message: string,
    public retryAfter?: number,
    public rateLimitRemaining?: number,
    public rateLimitReset?: number,
    status: number = 429,
    data?: unknown
  ) {
    super(message, status, data);
    this.name = 'RateLimitError';
  }
}
```

- When status 429 is received, `apiClient` parses `Retry-After`, `RateLimit-Remaining`, and `RateLimit-Reset` headers and raises a typed `RateLimitError`.
- **Anti-Looping Protection**: Unlike 401 token refresh retries, 429 responses are never automatically retried in tight loops, preventing cascading client storms.
- UI components can catch `RateLimitError` to present informative toast notifications indicating when the user may retry.

---

## 7. Configuration Reference

```yaml
buildsync:
  rate-limit:
    enabled: ${RATE_LIMIT_ENABLED:true}
    storage: ${RATE_LIMIT_STORAGE:memory} # memory | redis
    trusted-proxy-enabled: ${TRUSTED_PROXY_ENABLED:false}
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout-ms: ${REDIS_TIMEOUT_MS:2000}
    policies:
      general-authenticated:
        capacity: 120
        refill-tokens: 120
        refill-period-seconds: 60
      general-anonymous:
        capacity: 60
        refill-tokens: 60
        refill-period-seconds: 60
      auth-login:
        capacity: 10
        refill-tokens: 10
        refill-period-seconds: 300
      auth-register:
        capacity: 5
        refill-tokens: 5
        refill-period-seconds: 3600
      auth-refresh:
        capacity: 30
        refill-tokens: 30
        refill-period-seconds: 900
      search-developers:
        capacity: 60
        refill-tokens: 60
        refill-period-seconds: 60
      search-projects:
        capacity: 60
        refill-tokens: 60
        refill-period-seconds: 60
      avatar-upload:
        capacity: 10
        refill-tokens: 10
        refill-period-seconds: 3600
      collaboration-actions:
        capacity: 20
        refill-tokens: 20
        refill-period-seconds: 60
      direct-messaging:
        capacity: 30
        refill-tokens: 30
        refill-period-seconds: 60
      project-group-messaging:
        capacity: 30
        refill-tokens: 30
        refill-period-seconds: 60
      websocket-connect:
        capacity: 10
        refill-tokens: 10
        refill-period-seconds: 60
      typing-events:
        capacity: 60
        refill-tokens: 60
        refill-period-seconds: 60
      github-sync:
        capacity: 5
        refill-tokens: 5
        refill-period-seconds: 3600
```

