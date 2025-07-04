-- AI配置表
create table if not exists SY_AI_CONFIG
(
  id                      varchar(200)  not null unique primary key comment 'AI配置唯一id',
  name                    varchar(200)  not null comment 'AI配置名称',
  model_type              varchar(50)   not null comment 'AI模型类型',
  api_url                 varchar(500)  not null comment 'API地址',
  api_key                 varchar(500)  not null comment 'API密钥',
  model_name              varchar(200) comment '模型名称',
  remark                  varchar(500) comment 'AI配置描述',
  status                  varchar(200)  not null comment '状态',
  create_by               varchar(200)  not null comment '创建人',
  create_date_time        datetime      not null comment '创建时间',
  last_modified_by        varchar(200)  not null comment '更新人',
  last_modified_date_time datetime      not null comment '更新时间',
  version_number          int           not null comment '版本号',
  deleted                 int default 0 not null comment '逻辑删除',
  tenant_id               varchar(200)  not null comment '租户id'
);

-- 为作业配置表添加AI配置ID字段
alter table SY_WORK_CONFIG add column ai_config_id varchar(200) comment 'AI配置ID';
