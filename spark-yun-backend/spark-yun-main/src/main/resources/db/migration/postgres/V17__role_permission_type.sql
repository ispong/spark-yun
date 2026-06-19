ALTER TABLE sy_role_permission ADD COLUMN permission_type VARCHAR(50) DEFAULT 'API' NOT NULL;

UPDATE sy_role_permission SET permission_type = 'MENU' WHERE permission_code LIKE '%:menu';

CREATE INDEX idx_sy_role_permission_type ON sy_role_permission (tenant_id, role_id, permission_type);
