ALTER TABLE sy_platform_setting ADD COLUMN auto_create_tenant BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE sy_login_method_config ADD COLUMN account_password_enabled BOOLEAN DEFAULT TRUE NOT NULL;

CREATE TABLE IF NOT EXISTS sy_login_log
(
  id VARCHAR(200) NOT NULL,
  login_method VARCHAR(100) NOT NULL,
  account_identifier VARCHAR(200) NOT NULL,
  user_id VARCHAR(200),
  ip_address VARCHAR(200),
  user_agent VARCHAR(1000),
  login_status VARCHAR(100) NOT NULL,
  registered BOOLEAN DEFAULT FALSE NOT NULL,
  error_message VARCHAR(1000),
  create_by VARCHAR(200) NOT NULL,
  create_date_time TIMESTAMP NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time TIMESTAMP NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_sy_login_log_query ON sy_login_log (login_method, login_status, create_date_time);
