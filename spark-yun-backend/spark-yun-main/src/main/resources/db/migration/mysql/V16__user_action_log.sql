ALTER TABLE sy_platform_setting
  ADD COLUMN user_log_enabled BOOLEAN DEFAULT FALSE NOT NULL,
  ADD COLUMN user_log_retention_days INT DEFAULT 180 NOT NULL;

ALTER TABLE sy_user_action
  ADD COLUMN module_code VARCHAR(100),
  ADD COLUMN module_name VARCHAR(200),
  ADD COLUMN log_type VARCHAR(100),
  ADD COLUMN action_code VARCHAR(100),
  ADD COLUMN action_name VARCHAR(200),
  ADD COLUMN api_name VARCHAR(200),
  ADD COLUMN ip_address VARCHAR(200),
  ADD COLUMN user_agent VARCHAR(1000),
  ADD COLUMN status VARCHAR(100),
  ADD COLUMN duration BIGINT,
  ADD COLUMN exception_message LONGTEXT;

ALTER TABLE sy_user_action MODIFY COLUMN req_header LONGTEXT;

CREATE INDEX idx_sy_user_action_query ON sy_user_action (log_type, module_code, status, create_date_time);
