-- 单点登录信息表
create table SY_WORK_EVENT
(
    id                      varchar(200)  not null comment '事件id' primary key,
    event_type              varchar(200)  not null comment '事件类型',
    work_instance_id        varchar(200)  not null comment '作业实例id',
    workflow_id             varchar(200)  not null comment '作业流id',
    check_status            varchar(200)  not null comment '作业检查状态',
    file_status             varchar(2000) not null comment '上传文件是否完成',
    request_status          varchar(200)  not null comment '封装请求体是否完成',
    exec_status             varchar(200)  not null comment '作业执行是否完成',
    work_status             varchar(200)  not null comment '作业状态获取是否完成',
    data_log_status         varchar(200)  not null comment '日志数据是否完成',
    exec_req                varchar(200)  not null comment '封装执行请求体',
    status_req              varchar(2000) not null comment '封装获取状态请求体',
    log_req                 varchar(2000) not null comment '封装获取状态请求体',
    data_req                varchar(2000) not null comment '封装获取状态请求体',
    event_context           varchar(2000) not null comment '整个事件上下文',
    create_by               varchar(200)  not null comment '创建人',
    create_date_time        datetime      not null comment '创建时间',
    last_modified_by        varchar(200)  not null comment '更新人',
    last_modified_date_time datetime      not null comment '更新时间',
    version_number          int           not null comment '版本号',
    deleted                 int default 0 not null comment '逻辑删除'
);