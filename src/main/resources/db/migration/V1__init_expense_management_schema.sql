-- =============================================================
-- V1__init_expense_management_schema.sql
-- Initial database schema for User Management and Expense System
-- =============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================
--  Base entities
-- =========================
-- Common columns handled via inheritance in JPA (BaseEntity)

-- =========================
--  Table: um_users
-- =========================
CREATE TABLE um_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    address VARCHAR(255),
    department VARCHAR(255),
    project VARCHAR(255),
    employee_id varchar(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- =========================
--  Table: roles
-- =========================
CREATE TABLE role (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    is_deleted BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- =========================
--  Table: user_roles (Join Table)
-- =========================
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES um_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- =========================
--  Table: expenses
-- =========================
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    expense_date DATE NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    receipt_url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    requested_by UUID NOT NULL REFERENCES um_users(id),
    approved_by UUID REFERENCES um_users(id),

    rejection_reason VARCHAR(500),
    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- =========================
--  Table: blacklisted_tokens
-- =========================
CREATE TABLE blacklisted_token (
    token VARCHAR(500) PRIMARY KEY,
    expiry_time TIMESTAMP NOT NULL
);

