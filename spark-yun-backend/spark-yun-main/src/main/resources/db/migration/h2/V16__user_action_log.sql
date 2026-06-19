ALTER TABLE sy_platform_setting ADD COLUMN user_log_enabled BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE sy_platform_setting ADD COLUMN user_log_retention_days INT DEFAULT 180 NOT NULL;

ALTER TABLE sy_user_action ADD COLUMN module_code VARCHAR(100);
ALTER TABLE sy_user_action ADD COLUMN module_name VARCHAR(200);
ALTER TABLE sy_user_action ADD COLUMN log_type VARCHAR(100);
ALTER TABLE sy_user_action ADD COLUMN action_code VARCHAR(100);
ALTER TABLE sy_user_action ADD COLUMN action_name VARCHAR(200);
ALTER TABLE sy_user_action ADD COLUMN api_name VARCHAR(200);
ALTER TABLE sy_user_action ADD COLUMN ip_address VARCHAR(200);
ALTER TABLE sy_user_action ADD COLUMN user_agent VARCHAR(1000);
ALTER TABLE sy_user_action ADD COLUMN status VARCHAR(100);
ALTER TABLE sy_user_action ADD COLUMN duration BIGINT;
ALTER TABLE sy_user_action ADD COLUMN exception_message CLOB;
ALTER TABLE sy_user_action ALTER COLUMN req_header CLOB;

CREATE INDEX idx_sy_user_action_query ON sy_user_action (log_type, module_code, status, create_date_time);
