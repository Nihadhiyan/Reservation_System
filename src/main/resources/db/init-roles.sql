-- ============================================================================
-- Initialization Script: Least Privilege Database Role Hardening (PostgreSQL)
-- ============================================================================

-- 1. Revoke default public schema permissions to prevent unauthorized access
REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE bookfairdb FROM PUBLIC;

-- 2. Create restricted application user role (not superuser, cannot create DBs/roles)
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'bookfair_app') THEN
      CREATE ROLE bookfair_app LOGIN PASSWORD 'ReplaceWithStrongSecurePasswordFromVault';
   END IF;
END
$do$;

-- Explicitly enforce role privileges
ALTER ROLE bookfair_app NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;

-- 3. Grant database connection permission to application role
GRANT CONNECT ON DATABASE bookfairdb TO bookfair_app;

-- 4. Grant usage permission on public schema
GRANT USAGE ON SCHEMA public TO bookfair_app;

-- 5. Grant ONLY least-privilege DML operations (SELECT, INSERT, UPDATE, DELETE) on existing tables
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO bookfair_app;

-- 6. Grant usage and update privileges on sequences (required for ID/serial generation)
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO bookfair_app;

-- 7. Ensure future tables created by schema owner automatically grant only DML privileges to bookfair_app
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bookfair_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO bookfair_app;

-- Note: No DDL permissions (CREATE, DROP, ALTER) are granted to bookfair_app on the public schema.
