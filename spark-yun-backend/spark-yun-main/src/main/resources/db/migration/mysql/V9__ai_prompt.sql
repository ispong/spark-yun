CREATE TABLE sy_ai_prompt
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  name VARCHAR(200) NOT NULL,
  content LONGTEXT NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sy_ai_prompt_tenant ON sy_ai_prompt (tenant_id);
