ALTER TABLE sy_tenant_users
  ADD COLUMN apply_role_ids VARCHAR(1000),
  ADD COLUMN apply_invite_code VARCHAR(64);

CREATE TABLE sy_tenant_invite_code
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  invite_code VARCHAR(64) NOT NULL,
  valid_days INT,
  expire_date_time TIMESTAMP,
  role_ids VARCHAR(1000),
  create_by VARCHAR(200) NOT NULL,
  create_date_time TIMESTAMP NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time TIMESTAMP NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_sy_tenant_invite_code_code ON sy_tenant_invite_code (invite_code);
CREATE INDEX idx_sy_tenant_invite_code_tenant ON sy_tenant_invite_code (tenant_id);
