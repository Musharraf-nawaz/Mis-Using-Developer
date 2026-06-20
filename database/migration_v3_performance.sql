-- Performance indexes

CREATE INDEX IF NOT EXISTS idx_notifications_user_type_entity
    ON notifications(user_id, type, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read
    ON notifications(user_id, read);
CREATE INDEX IF NOT EXISTS idx_project_user_project
    ON project_user_assignments(project_id);
CREATE INDEX IF NOT EXISTS idx_interviews_date_status
    ON interviews(interview_date, interview_status);
CREATE INDEX IF NOT EXISTS idx_assets_assigned_to_status
    ON assets(assigned_to_id, status);
