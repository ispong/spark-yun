ALTER TABLE sy_platform_setting ADD COLUMN default_tenant_member_num INT DEFAULT 5 NOT NULL;
ALTER TABLE sy_platform_setting ADD COLUMN default_tenant_workflow_num INT DEFAULT 10 NOT NULL;
ALTER TABLE sy_platform_setting ADD COLUMN default_tenant_valid_days INT DEFAULT 7 NOT NULL;
