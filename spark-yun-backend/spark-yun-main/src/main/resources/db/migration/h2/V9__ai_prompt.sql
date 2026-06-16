CREATE TABLE sy_ai_prompt
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  name VARCHAR(200) NOT NULL,
  content CLOB NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time TIMESTAMP NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time TIMESTAMP NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_sy_ai_prompt_tenant ON sy_ai_prompt (tenant_id);
