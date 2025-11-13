-- =============================================================
-- V2__insert_roles.sql
-- Seed default roles
-- =============================================================

INSERT INTO role (id, name, description)
VALUES
    (uuid_generate_v4(), 'EMPLOYEE', 'Regular employee role'),
    (uuid_generate_v4(), 'FINANCE_ADMIN', 'Finance admin with approval permissions'),
    (uuid_generate_v4(), 'SUPER_ADMIN', 'Super Administrator with full system access')
ON CONFLICT (name) DO NOTHING;
