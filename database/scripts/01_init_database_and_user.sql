-- ==============================================================================
-- BuildSync - Database Initialization Script
-- Execute as PostgreSQL superuser (e.g., 'postgres')
-- ==============================================================================

-- 1. Create dedicated application user
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'buildsync_app') THEN
      CREATE ROLE buildsync_app WITH LOGIN PASSWORD 'buildsync_dev_password';
   END IF;
END
$do$;

-- 2. Create application database owned by buildsync_app
SELECT 'CREATE DATABASE buildsync_db OWNER buildsync_app'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'buildsync_db')\gexec

-- 3. Grant schema permissions
\connect buildsync_db

GRANT ALL ON SCHEMA public TO buildsync_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO buildsync_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO buildsync_app;
