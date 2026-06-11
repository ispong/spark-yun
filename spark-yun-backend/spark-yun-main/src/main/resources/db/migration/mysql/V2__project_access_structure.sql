ALTER TABLE sy_user ADD COLUMN platform_admin BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE sy_tenant ADD COLUMN admin_user_id VARCHAR(200);
ALTER TABLE sy_tenant_users ADD COLUMN normal_admin BOOLEAN DEFAULT FALSE NOT NULL;

CREATE TABLE sy_role
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  name VARCHAR(200) NOT NULL,
  code VARCHAR(200) NOT NULL,
  status VARCHAR(100) NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sy_role_permission
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  role_id VARCHAR(200) NOT NULL,
  permission_code VARCHAR(200) NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sy_member_role
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  user_id VARCHAR(200) NOT NULL,
  role_id VARCHAR(200) NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sy_org
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  parent_id VARCHAR(200),
  name VARCHAR(200) NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sy_org_member
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  org_id VARCHAR(200) NOT NULL,
  user_id VARCHAR(200) NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sy_org_role
(
  id VARCHAR(200) NOT NULL,
  tenant_id VARCHAR(200) NOT NULL,
  org_id VARCHAR(200) NOT NULL,
  role_id VARCHAR(200) NOT NULL,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sy_role_tenant ON sy_role (tenant_id);
CREATE INDEX idx_sy_role_permission_role ON sy_role_permission (tenant_id, role_id);
CREATE INDEX idx_sy_member_role_user ON sy_member_role (tenant_id, user_id);
CREATE INDEX idx_sy_org_tenant ON sy_org (tenant_id);
CREATE INDEX idx_sy_org_member_user ON sy_org_member (tenant_id, user_id);
CREATE INDEX idx_sy_org_role_org ON sy_org_role (tenant_id, org_id);
