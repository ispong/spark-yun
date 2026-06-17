CREATE TABLE IF NOT EXISTS sy_login_method_config
(
  id VARCHAR(200) NOT NULL,
  config_key VARCHAR(200) NOT NULL,
  default_login_method VARCHAR(100) DEFAULT 'ACCOUNT' NOT NULL,
  account_enabled BOOLEAN DEFAULT TRUE NOT NULL,
  account_phone_password_enabled BOOLEAN DEFAULT TRUE NOT NULL,
  account_email_password_enabled BOOLEAN DEFAULT TRUE NOT NULL,
  email_enabled BOOLEAN DEFAULT FALSE NOT NULL,
  email_register_enabled BOOLEAN DEFAULT FALSE NOT NULL,
  phone_enabled BOOLEAN DEFAULT FALSE NOT NULL,
  phone_register_enabled BOOLEAN DEFAULT FALSE NOT NULL,
  auto_create_tenant BOOLEAN DEFAULT TRUE NOT NULL,
  config_json LONGTEXT,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX idx_sy_login_method_config_key ON sy_login_method_config (config_key);

CREATE TABLE IF NOT EXISTS sy_login_code_record
(
  id VARCHAR(200) NOT NULL,
  channel VARCHAR(100) NOT NULL,
  receiver VARCHAR(200) NOT NULL,
  scene VARCHAR(100) NOT NULL,
  code_hash VARCHAR(200),
  expire_date_time DATETIME,
  send_status VARCHAR(100) NOT NULL,
  verify_status VARCHAR(100) NOT NULL,
  verify_fail_count INT DEFAULT 0 NOT NULL,
  registered BOOLEAN DEFAULT FALSE NOT NULL,
  auto_tenant_created BOOLEAN DEFAULT FALSE NOT NULL,
  error_message VARCHAR(1000),
  provider_message LONGTEXT,
  verify_date_time DATETIME,
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sy_login_code_receiver ON sy_login_code_record (channel, receiver, scene, create_date_time);

INSERT INTO sy_login_method_config (
  id, config_key, default_login_method, account_enabled, account_phone_password_enabled, account_email_password_enabled,
  email_enabled, email_register_enabled, phone_enabled, phone_register_enabled, auto_create_tenant,
  config_json, create_by, create_date_time, last_modified_by, last_modified_date_time, version_number, deleted
)
SELECT
  'sy_login_method_config_default', 'GLOBAL', 'ACCOUNT', TRUE, TRUE, TRUE,
  FALSE, FALSE, FALSE, FALSE, TRUE,
  '{"emailConfig":{},"phoneConfig":{"provider":"ALIYUN","regionId":"cn-hangzhou","templateParamName":"code"}}',
  'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sy_login_method_config WHERE config_key = 'GLOBAL');
