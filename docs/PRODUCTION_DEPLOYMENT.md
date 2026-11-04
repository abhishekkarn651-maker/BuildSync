# BuildSync — Production Deployment Guide

## 1. Supported Deployment Architectures

BuildSync supports two primary production deployment topologies:

1. **Containerized Multi-Service (Recommended):**
   - Managed via Docker Compose or Kubernetes (EKS / GKE / AKS / Nomad).
   - Dedicated containers for Frontend (`buildsync-frontend`), Backend (`buildsync-backend`), PostgreSQL 17, and Redis 7.
   - Ideal for scalability, reproducible builds, and automated rolling deployments.
2. **Hybrid Cloud / Managed Services:**
   - Frontend hosted on Vercel / Cloudflare Pages / AWS ECS.
   - Backend hosted on AWS ECS Fargate / Google Cloud Run / DigitalOcean App Platform.
   - Database on AWS RDS PostgreSQL / Google Cloud SQL / Neon.
   - Cache on AWS ElastiCache Redis / Redis Cloud.

```
                              [ Internet Clients ]
                                       │
                                       ▼ (HTTPS 443)
                          [ Reverse Proxy / Cloudflare ]
                                       │
                     ┌─────────────────┴─────────────────┐
                     ▼                                   ▼
        [ Next.js 16 Web App ]              [ Spring Boot 3 Backend ]
             (Port 3000)                         (Port 8080)
                                                 │       │
                                     ┌───────────┘       └───────────┐
                                     ▼                               ▼
                          [ PostgreSQL 17 DB ]               [ Redis 7 Cluster ]
                              (Port 5432)                        (Port 6379)
```

---

## 2. Environment Variables Reference

Prior to production launch, create a hardened `.env` file on the deployment host. **Never commit production secrets to Git.**

```dotenv
# ==============================================================================
# BuildSync Production Environment Configuration
# ==============================================================================

# Application Profile
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
NODE_ENV=production
NEXT_TELEMETRY_DISABLED=1

# PostgreSQL 17 Database
DB_URL=jdbc:postgresql://postgres.internal:5432/buildsync_prod
DB_USERNAME=buildsync_app
DB_PASSWORD=REPLACE_WITH_STRONG_RANDOM_PASSWORD_MIN_32_CHARS
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5

# Redis 7 Distributed Rate Limiting & Shared State
RATE_LIMIT_ENABLED=true
RATE_LIMIT_STORAGE=redis
TRUSTED_PROXY_ENABLED=true
REDIS_HOST=redis.internal
REDIS_PORT=6379
REDIS_PASSWORD=REPLACE_WITH_REDIS_AUTH_TOKEN
REDIS_TIMEOUT_MS=2000

# Cryptographic Secrets
# Generate using: openssl rand -base64 48
JWT_SECRET=REPLACE_WITH_CRYPTOGRAPHICALLY_SECURE_256BIT_SECRET_MIN_32_BYTES
JWT_EXPIRATION_SECONDS=900

# Cookie Security (HTTPS Required)
COOKIE_SECURE=true
COOKIE_SAME_SITE=Lax
REFRESH_TOKEN_NORMAL_DAYS=7
REFRESH_TOKEN_REMEMBER_ME_DAYS=30

# CORS Allowed Origins (Comma-separated exact production domains)
CORS_ALLOWED_ORIGINS=https://buildsync.dev,https://www.buildsync.dev
NEXT_PUBLIC_API_URL=https://api.buildsync.dev

# Storage Directory for Avatars & Media
STORAGE_UPLOAD_DIR=/var/buildsync/uploads
STORAGE_AVATAR_SUBDIR=avatars

# GitHub App Integration
GITHUB_APP_CLIENT_ID=REPLACE_WITH_PRODUCTION_GITHUB_APP_CLIENT_ID
GITHUB_APP_CLIENT_SECRET=REPLACE_WITH_PRODUCTION_GITHUB_APP_CLIENT_SECRET
GITHUB_APP_CALLBACK_URL=https://api.buildsync.dev/api/v1/integrations/github/callback
GITHUB_FRONTEND_REDIRECT_URL=https://buildsync.dev/settings/integrations/github
# Generate using: openssl rand -base64 32
GITHUB_TOKEN_ENCRYPTION_KEY=REPLACE_WITH_AES_256_GCM_KEY_EXACTLY_32_BYTES
```

---

## 3. Docker Build & Deployment Instructions

### 1. Build Container Images
From the repository root (`D:\BUILDSYNC`):

```bash
# Build Backend Image
docker build -t buildsync/backend:1.0.0 -f backend/Dockerfile ./backend

# Build Frontend Image
docker build -t buildsync/frontend:1.0.0 -f frontend/Dockerfile ./frontend
```

### 2. Launch Full Stack with Docker Compose
```bash
# Start all services detached
docker compose up -d

# Verify container health status
docker compose ps
```

---

## 4. Reverse Proxy & HTTPS Configuration (NGINX Example)

Deploy an NGINX reverse proxy to terminate TLS and forward requests to the appropriate internal services.

```nginx
# NGINX Production Configuration for BuildSync

# 1. API & WebSocket Service (api.buildsync.dev)
server {
    listen 443 ssl http2;
    server_name api.buildsync.dev;

    ssl_certificate /etc/letsencrypt/live/api.buildsync.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.buildsync.dev/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    client_max_body_size 6M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket & STOMP Upgrade Support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 86400s;
        proxy_send_timeout 86400s;
    }
}

# 2. Web Application Frontend (buildsync.dev)
server {
    listen 443 ssl http2;
    server_name buildsync.dev www.buildsync.dev;

    ssl_certificate /etc/letsencrypt/live/buildsync.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/buildsync.dev/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 5. PostgreSQL Database Operations

### 1. Production User & Permissions
Connect to PostgreSQL as `postgres` superuser and provision a dedicated non-superuser role:

```sql
CREATE DATABASE buildsync_prod;
CREATE USER buildsync_app WITH ENCRYPTED PASSWORD 'REPLACE_WITH_PASSWORD';

-- Grant required schema privileges
GRANT CONNECT ON DATABASE buildsync_prod TO buildsync_app;
\c buildsync_prod
GRANT USAGE, CREATE ON SCHEMA public TO buildsync_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO buildsync_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO buildsync_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO buildsync_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO buildsync_app;
```

### 2. Flyway Migration Execution
Migrations `V1` through `V10` execute automatically on Spring Boot application boot when `spring.flyway.enabled=true`.

To run migrations manually via Maven:
```bash
./mvnw -f backend/pom.xml flyway:migrate -Dflyway.url=jdbc:postgresql://postgres.internal:5432/buildsync_prod -Dflyway.user=buildsync_app -Dflyway.password=SECRET
```

### 3. Automated Database Backup
Create a daily cron job executing `pg_dump`:
```bash
#!/bin/bash
BACKUP_DIR="/var/backups/buildsync"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
mkdir -p "$BACKUP_DIR"

pg_dump -h localhost -U buildsync_app -d buildsync_prod -Fc -f "$BACKUP_DIR/buildsync_backup_${TIMESTAMP}.dump"
find "$BACKUP_DIR" -type f -mtime +30 -delete
```

### 4. Database Restoration
To restore a snapshot:
```bash
pg_restore -h localhost -U buildsync_app -d buildsync_prod --clean --if-exists /var/backups/buildsync/buildsync_backup_20261103.dump
```

---

## 6. GitHub OAuth Production Setup

1. Navigate to **GitHub Developer Settings** -> **GitHub Apps** -> Your BuildSync App.
2. Set **Homepage URL** to `https://buildsync.dev`.
3. Set **Callback URL** to `https://api.buildsync.dev/api/v1/integrations/github/callback`.
4. Ensure requested permissions:
   - Repository Permissions: `Metadata` (Read-only).
5. Generate a Client Secret and set `GITHUB_APP_CLIENT_SECRET` in `.env`.

---

## 7. Health Monitoring & Rollback Procedure

### Health Endpoints
- **Public System Health:** `GET https://api.buildsync.dev/api/v1/health`
  - Responds with `200 OK` and database connectivity status.
- **Actuator Health:** `GET https://api.buildsync.dev/actuator/health`

### Rollback Procedure
If a production issue arises after deployment:
1. **Revert Frontend:** Roll back frontend container or deployment to the previous image tag (`buildsync/frontend:previous`).
2. **Revert Backend:** Revert backend container image to (`buildsync/backend:previous`).
3. **Database Compatibility:** All BuildSync Flyway migrations are designed to be additive and backward-compatible. In the rare event of a schema mismatch, restore the pre-migration `pg_dump` snapshot.

