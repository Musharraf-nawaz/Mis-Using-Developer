-- Migration v2: Projects, Interview Rounds, Asset Media, RBAC USER role

-- Role migration: HR/EMPLOYEE -> USER
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
UPDATE users SET role = 'USER' WHERE role IN ('HR', 'EMPLOYEE');
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'USER'));

-- Projects
CREATE TABLE IF NOT EXISTS projects (
    id                          BIGSERIAL PRIMARY KEY,
    project_name                VARCHAR(200) NOT NULL,
    client_name                 VARCHAR(150) NOT NULL,
    mid_client_name             VARCHAR(150),
    candidate_working_count     INT NOT NULL DEFAULT 0,
    interview_candidate_count   INT NOT NULL DEFAULT 0,
    onboarded_candidate_count   INT NOT NULL DEFAULT 0,
    start_date                  DATE,
    end_date                    DATE,
    budget                      DECIMAL(14, 2),
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                CHECK (status IN ('ACTIVE', 'COMPLETED', 'ON_HOLD', 'CANCELLED')),
    remarks                     TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_user_assignments (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, user_id)
);

-- Asset enhancements
ALTER TABLE assets ADD COLUMN IF NOT EXISTS warranty_expiry_date DATE;
ALTER TABLE assets ADD COLUMN IF NOT EXISTS vendor_name VARCHAR(150);

CREATE TABLE IF NOT EXISTS asset_media (
    id          BIGSERIAL PRIMARY KEY,
    asset_id    BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    media_type  VARCHAR(20) NOT NULL CHECK (media_type IN ('PHOTO', 'VIDEO')),
    file_url    VARCHAR(500) NOT NULL,
    file_name   VARCHAR(255),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Interview enhancements
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS client_name VARCHAR(150);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS mid_client_name VARCHAR(150);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS company_to_represent VARCHAR(150);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS interview_link VARCHAR(500);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS candidate_cv_url VARCHAR(500);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS final_status VARCHAR(20)
    CHECK (final_status IS NULL OR final_status IN ('SELECTED', 'REJECTED', 'ON_HOLD'));

CREATE TABLE IF NOT EXISTS interview_rounds (
    id                  BIGSERIAL PRIMARY KEY,
    interview_id        BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    round_number        INT NOT NULL CHECK (round_number BETWEEN 1 AND 3),
    interview_link      VARCHAR(500),
    interview_date      DATE,
    interview_time      TIME,
    company_to_represent VARCHAR(150),
    interviewer         VARCHAR(150),
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                        CHECK (status IN ('SCHEDULED', 'PASSED', 'FAILED', 'CANCELLED')),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(interview_id, round_number)
);

CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_project_user_user ON project_user_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_interview_rounds_interview ON interview_rounds(interview_id);
CREATE INDEX IF NOT EXISTS idx_asset_media_asset ON asset_media(asset_id);
