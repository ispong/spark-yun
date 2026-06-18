ALTER TABLE sy_cluster_node ADD COLUMN IF NOT EXISTS connect_type VARCHAR(200) DEFAULT 'SSH';

COMMENT ON COLUMN sy_cluster_node.connect_type IS '节点连接方式';

UPDATE sy_cluster_node SET connect_type = 'SSH' WHERE connect_type IS NULL;

ALTER TABLE sy_cluster_node ALTER COLUMN port DROP NOT NULL;
ALTER TABLE sy_cluster_node ALTER COLUMN username DROP NOT NULL;
ALTER TABLE sy_cluster_node ALTER COLUMN passwd DROP NOT NULL;
ALTER TABLE sy_cluster_node ALTER COLUMN agent_home_path DROP NOT NULL;
