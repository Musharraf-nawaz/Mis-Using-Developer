-- Run this if you already executed migration_v2.sql before mid_client_name was added
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS mid_client_name VARCHAR(150);
