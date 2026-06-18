ALTER TABLE sy_cluster_node
  ADD COLUMN connect_type VARCHAR(200) DEFAULT 'SSH' COMMENT '节点连接方式';

UPDATE sy_cluster_node SET connect_type = 'SSH' WHERE connect_type IS NULL;

ALTER TABLE sy_cluster_node
  MODIFY COLUMN port INT NULL COMMENT '节点服务器端口号',
  MODIFY COLUMN username VARCHAR(200) NULL COMMENT '节点服务器用户名',
  MODIFY COLUMN passwd VARCHAR(5000) NULL COMMENT '节点服务器密码',
  MODIFY COLUMN agent_home_path VARCHAR(200) NULL COMMENT '至轻云代理安装目录';
