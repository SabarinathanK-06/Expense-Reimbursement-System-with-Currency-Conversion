-- =============================================================
-- Migration: Drop unwanted columns from expense, role, and um_users
-- Author: Sabarinathan
-- Date: 2025-11-11
-- =============================================================

-- === Drop columns from expense table ===
ALTER TABLE IF EXISTS expense
    DROP COLUMN IF EXISTS deleted_at,
    DROP COLUMN IF EXISTS amount_in_inr,
    DROP COLUMN IF EXISTS approved_at;

-- === Drop column from role table ===
ALTER TABLE IF EXISTS role
    DROP COLUMN IF EXISTS deleted_at;

-- === Drop column from um_users table ===
ALTER TABLE IF EXISTS um_users
    DROP COLUMN IF EXISTS deleted_at;