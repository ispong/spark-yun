CREATE TABLE sy_ai_chat_session
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  user_id VARCHAR(200) NOT NULL,
  config_id VARCHAR(200) NOT NULL,
  title VARCHAR(200) NOT NULL,
  messages_json LONGTEXT NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sy_ai_chat_session_tenant_user ON sy_ai_chat_session (tenant_id, user_id);
