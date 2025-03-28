-- 作业事件表
create table SY_WORK_EVENT
(
    id                      varchar(200)  not null comment '事件id' primary key,
    exec_process            int           not null comment '进程状态',
    exec_work_req           text          null comment '封装执行作业请求体',
    get_status_req          text          null comment '封装获取状态请求体',
    get_log_req             text          null comment '封装获取日志请求体',
    get_data_req            text          null comment '封装获取数据请求体',
    create_by               varchar(200)  not null comment '创建人',
    create_date_time        datetime      not null comment '创建时间',
    last_modified_by        varchar(200)  not null comment '更新人',
    last_modified_date_time datetime      not null comment '更新时间',
    version_number          int           not null comment '版本号',
    deleted                 int default 0 not null comment '逻辑删除',
    tenant_id               varchar(200)  not null comment '租户id'
);