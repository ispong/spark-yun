package com.isxcode.star.modules.work.run.impl;

import com.isxcode.star.api.work.constants.WorkType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.isxcode.star.modules.alarm.service.AlarmService;
import com.isxcode.star.modules.datasource.mapper.DatasourceMapper;
import com.isxcode.star.modules.datasource.repository.DatasourceRepository;
import com.isxcode.star.modules.datasource.source.DataSourceFactory;
import com.isxcode.star.modules.work.repository.WorkInstanceRepository;
import com.isxcode.star.modules.work.sql.SqlCommentService;
import com.isxcode.star.modules.work.sql.SqlFunctionService;
import com.isxcode.star.modules.work.sql.SqlValueService;
import com.isxcode.star.modules.workflow.repository.WorkflowInstanceRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ImpalaExecutorTest {

    @Mock
    private WorkInstanceRepository workInstanceRepository;

    @Mock
    private DatasourceRepository datasourceRepository;

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Mock
    private SqlCommentService sqlCommentService;

    @Mock
    private SqlValueService sqlValueService;

    @Mock
    private SqlFunctionService sqlFunctionService;

    @Mock
    private AlarmService alarmService;

    @Mock
    private DataSourceFactory dataSourceFactory;

    @Mock
    private DatasourceMapper datasourceMapper;

    @InjectMocks
    private ImpalaExecutor impalaExecutor;

    @Test
    void testGetWorkType() {
        // 测试工作类型是否正确返回
        String workType = impalaExecutor.getWorkType();
        assertEquals(WorkType.QUERY_IMPALA, workType);
    }
}
