-- 作业事件表
create table SY_WORK_EVENT
(
    id                      varchar(200)  not null comment '事件id' primary key,
    exec_process            int           not null comment '进程状态',
    event_body              text          null comment '封装作业临时请求体',
    create_by               varchar(200)  not null comment '创建人',
    create_date_time        datetime      not null comment '创建时间',
    last_modified_by        varchar(200)  not null comment '更新人',
    last_modified_date_time datetime      not null comment '更新时间'
);