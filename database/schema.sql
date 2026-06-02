-- Asset & Interview Management System - PostgreSQL Schema

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'HR', 'EMPLOYEE')),
    department      VARCHAR(100),
    phone           VARCHAR(20),
    employee_id     VARCHAR(50) UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- Assets
CREATE TABLE assets (
    id              BIGSERIAL PRIMARY KEY,
    company_name    VARCHAR(150) NOT NULL,
    asset_name      VARCHAR(200) NOT NULL,
    associated_developer VARCHAR(150),
    project_name    VARCHAR(150),
    project_offboarded BOOLEAN NOT NULL DEFAULT FALSE,
    asset_category  VARCHAR(100) NOT NULL,
    asset_type      VARCHAR(100) NOT NULL,
    serial_number   VARCHAR(100) UNIQUE,
    asset_tag       VARCHAR(100) UNIQUE,
    purchase_date   DATE,
    purchase_cost   DECIMAL(12, 2),
    assigned_to_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_date   DATE,
    return_date     DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                    CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'RETURNED', 'DAMAGED', 'LOST')),
    condition       VARCHAR(50),
    remarks         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_company ON assets(company_name);
CREATE INDEX idx_assets_type ON assets(asset_type);
CREATE INDEX idx_assets_assigned_to ON assets(assigned_to_id);
CREATE INDEX idx_assets_tag ON assets(asset_tag);

-- Asset Assignments (history)
CREATE TABLE asset_assignments (
    id                      BIGSERIAL PRIMARY KEY,
    asset_id                BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    employee_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_name           VARCHAR(150) NOT NULL,
    employee_department     VARCHAR(100),
    assigned_date           DATE NOT NULL,
    expected_return_date    DATE,
    actual_return_date      DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'RETURNED', 'OVERDUE')),
    remarks                 TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_asset_assignments_asset ON asset_assignments(asset_id);
CREATE INDEX idx_asset_assignments_employee ON asset_assignments(employee_id);
CREATE INDEX idx_asset_assignments_status ON asset_assignments(status);

-- Interviews
CREATE TABLE interviews (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_name      VARCHAR(150) NOT NULL,
    candidate_email     VARCHAR(255) NOT NULL,
    candidate_phone     VARCHAR(20),
    candidate_profile     VARCHAR(200),
    skills              TEXT,
    experience          VARCHAR(50),
    interviewer_name    VARCHAR(150) NOT NULL,
    interviewer_email   VARCHAR(255),
    interviewer_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    interview_date      DATE NOT NULL,
    interview_time      TIME NOT NULL,
    interview_mode      VARCHAR(20) NOT NULL CHECK (interview_mode IN ('ONLINE', 'OFFLINE')),
    interview_round     VARCHAR(20) NOT NULL CHECK (interview_round IN ('SCREENING', 'FIRST_ROUND', 'SECOND_ROUND', 'THIRD_ROUND', 'HR', 'FINAL_ROUND')),
    interview_status    VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                        CHECK (interview_status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED')),
    feedback            TEXT,
    notes               TEXT,
    created_by_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interviews_date ON interviews(interview_date);
CREATE INDEX idx_interviews_status ON interviews(interview_status);
CREATE INDEX idx_interviews_candidate ON interviews(candidate_name);
CREATE INDEX idx_interviews_interviewer ON interviews(interviewer_name);

-- Notifications
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    type            VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    read            BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(read);

-- Audit Logs
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    action          VARCHAR(100) NOT NULL,
    user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    user_email      VARCHAR(255),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       BIGINT,
    old_value       TEXT,
    new_value       TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- Refresh Tokens
CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(500) NOT NULL UNIQUE,
    expiry_date     TIMESTAMP NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Seed default admin (password: Admin@123)
INSERT INTO users (full_name, email, password, role, department, phone, employee_id, status)
VALUES (
    'System Administrator',
    'admin@aims.com',
    '$2a$10$KRZG.MSJJSPhMTnD51.Mm.d5xw5sQ9U5AeIlqzkC3tYWbjEnhLpeC',
    'ADMIN',
    'IT',
    '+1-555-0100',
    'EMP001',
    'ACTIVE'
);

INSERT INTO users (full_name, email, password, role, department, phone, employee_id, status)
VALUES (
    'HR Manager',
    'hr@aims.com',
    '$2a$10$KRZG.MSJJSPhMTnD51.Mm.d5xw5sQ9U5AeIlqzkC3tYWbjEnhLpeC',
    'HR',
    'Human Resources',
    '+1-555-0101',
    'EMP002',
    'ACTIVE'
);

INSERT INTO users (full_name, email, password, role, department, phone, employee_id, status)
VALUES (
    'John Employee',
    'employee@aims.com',
    '$2a$10$KRZG.MSJJSPhMTnD51.Mm.d5xw5sQ9U5AeIlqzkC3tYWbjEnhLpeC',
    'EMPLOYEE',
    'Engineering',
    '+1-555-0102',
    'EMP003',
    'ACTIVE'
);
