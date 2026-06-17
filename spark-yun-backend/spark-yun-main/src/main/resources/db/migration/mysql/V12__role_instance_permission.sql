CREATE TABLE sy_role_instance_permission
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  role_id VARCHAR(200) NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  all_enabled BOOLEAN DEFAULT TRUE NOT NULL,
  resource_ids TEXT,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sy_role_instance_permission_role ON sy_role_instance_permission (tenant_id, role_id, resource_type);
