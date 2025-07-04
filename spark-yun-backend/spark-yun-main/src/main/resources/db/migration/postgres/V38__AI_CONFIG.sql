-- AI配置表
create table if not exists SY_AI_CONFIG
(
  id                      varchar(200)  not null unique primary key,
  name                    varchar(200)  not null,
  model_type              varchar(50)   not null,
  api_url                 varchar(500)  not null,
  api_key                 varchar(500)  not null,
  model_name              varchar(200),
  remark                  varchar(500),
  status                  varchar(200)  not null,
  create_by               varchar(200)  not null,
  create_date_time        timestamp     not null,
  last_modified_by        varchar(200)  not null,
  last_modified_date_time timestamp     not null,
  version_number          int           not null,
  deleted                 int default 0 not null,
  tenant_id               varchar(200)  not null
);

-- 为作业配置表添加AI配置ID字段
alter table SY_WORK_CONFIG add column ai_config_id varchar(200);
