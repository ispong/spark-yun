CREATE TABLE IF NOT EXISTS sy_platform_setting
(
  id VARCHAR(200) NOT NULL,
  setting_key VARCHAR(200) NOT NULL,
  description VARCHAR(2000),
  create_by VARCHAR(200) NOT NULL,
  create_date_time DATETIME NOT NULL,
  last_modified_by VARCHAR(200) NOT NULL,
  last_modified_date_time DATETIME NOT NULL,
  version_number BIGINT NOT NULL,
  deleted INT DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX idx_sy_platform_setting_key ON sy_platform_setting (setting_key);

INSERT INTO sy_platform_setting (
  id, setting_key, description,
  create_by, create_date_time, last_modified_by, last_modified_date_time, version_number, deleted
)
SELECT
  'sy_platform_setting_default', 'GLOBAL', '',
  'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sy_platform_setting WHERE setting_key = 'GLOBAL');
