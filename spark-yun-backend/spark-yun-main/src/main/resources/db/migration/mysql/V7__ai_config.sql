CREATE TABLE sy_ai_config
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  name VARCHAR(200) NOT NULL,
  provider_type VARCHAR(100) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  api_key VARCHAR(1000),
  model_name VARCHAR(200) NOT NULL,
  temperature DOUBLE DEFAULT 0.7 NOT NULL,
  max_tokens INT DEFAULT 2000 NOT NULL,
  status VARCHAR(100) NOT NULL,
  remark VARCHAR(500),
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sy_ai_config_tenant ON sy_ai_config (tenant_id);
