package com.isxcode.star.modules.work.run.impl;

import com.isxcode.star.api.work.constants.WorkLog;
import com.isxcode.star.api.work.constants.WorkType;
import com.isxcode.star.backend.api.base.exceptions.WorkRunException;
import com.isxcode.star.modules.alarm.service.AlarmService;
import com.isxcode.star.modules.datasource.entity.DatasourceEntity;
import com.isxcode.star.modules.datasource.mapper.DatasourceMapper;
import com.isxcode.star.modules.datasource.repository.DatasourceRepository;
import com.isxcode.star.modules.datasource.source.DataSourceFactory;
import com.isxcode.star.modules.work.entity.WorkInstanceEntity;
import com.isxcode.star.modules.work.repository.WorkInstanceRepository;
import com.isxcode.star.modules.work.run.WorkExecutor;
import com.isxcode.star.modules.work.run.WorkRunContext;
import com.isxcode.star.modules.work.sql.SqlCommentService;
import com.isxcode.star.modules.work.sql.SqlFunctionService;
import com.isxcode.star.modules.work.sql.SqlValueService;
import com.isxcode.star.modules.workflow.repository.WorkflowInstanceRepository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImpalaExecutor extends WorkExecutor {

    private final DatasourceRepository datasourceRepository;

    private final SqlCommentService sqlCommentService;

    private final SqlFunctionService sqlFunctionService;

    private final SqlValueService sqlValueService;

    private final DataSourceFactory dataSourceFactory;

    private final DatasourceMapper datasourceMapper;

    public ImpalaExecutor(WorkInstanceRepository workInstanceRepository, DatasourceRepository datasourceRepository,
        WorkflowInstanceRepository workflowInstanceRepository, SqlCommentService sqlCommentService,
        SqlValueService sqlValueService, SqlFunctionService sqlFunctionService, AlarmService alarmService,
        DataSourceFactory dataSourceFactory, DatasourceMapper datasourceMapper) {

        super(workInstanceRepository, workflowInstanceRepository, alarmService, sqlFunctionService);
        this.datasourceRepository = datasourceRepository;
        this.sqlCommentService = sqlCommentService;
        this.sqlValueService = sqlValueService;
        this.sqlFunctionService = sqlFunctionService;
        this.dataSourceFactory = dataSourceFactory;
        this.datasourceMapper = datasourceMapper;
    }

    @Override
    public String getWorkType() {
        return WorkType.QUERY_IMPALA;
    }

    public void execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance) {

        // 将线程存到Map
        WORK_THREAD.put(workInstance.getId(), Thread.currentThread());

        // 获取日志构造器
        StringBuilder logBuilder = workRunContext.getLogBuilder();

        // 检测数据源是否配置
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始检测运行环境 \n");
        if (Strings.isEmpty(workRunContext.getDatasourceId())) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测运行环境失败: 未配置有效数据源  \n");
        }

        // 检测数据源是否存在
        Optional<DatasourceEntity> datasourceEntityOptional =
            datasourceRepository.findById(workRunContext.getDatasourceId());
        if (!datasourceEntityOptional.isPresent()) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测运行环境失败: 未配置有效数据源  \n");
        }
        DatasourceEntity datasourceEntity = datasourceEntityOptional.get();

        // 检测脚本是否为空
        if (Strings.isEmpty(workRunContext.getScript())) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测运行环境失败: Impala查询脚本为空  \n");
        }

        // 检查通过
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始执行Impala查询 \n");
        workInstance = updateInstance(workInstance, logBuilder);

        // 解析sql
        String parseSql = workRunContext.getScript();

        // 先解析变量
        parseSql = sqlValueService.parseSqlValue(parseSql);

        // 再解析函数
        parseSql = sqlFunctionService.parseSqlFunction(parseSql);

        // 解析上游作业的数据
        parseSql = parseJsonPath(parseSql, workInstance);

        // 去除注释
        parseSql = sqlCommentService.removeSqlComment(parseSql);

        // 分割sql
        List<String> sqlList = Arrays.stream(parseSql.split(";")).filter(e -> !Strings.isEmpty(e.replace("\n", "")))
            .collect(Collectors.toList());

        if (sqlList.isEmpty()) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "Sql解析失败 : sql为空  \n");
        }

        // 开始执行sql
        try (Connection connection = dataSourceFactory.getDatasource(datasourceEntity.getDbType())
            .getConnection(datasourceMapper.datasourceEntityToConnectInfo(datasourceEntity))) {

            List<List<Object>> resultData = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            for (String sql : sqlList) {

                sql = sql.trim();
                if (Strings.isEmpty(sql)) {
                    continue;
                }

                logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始执行sql : ").append(sql)
                    .append(" \n");
                workInstance = updateInstance(workInstance, logBuilder);

                try (Statement statement = connection.createStatement()) {

                    boolean hasResultSet = statement.execute(sql);

                    if (hasResultSet) {
                        try (ResultSet resultSet = statement.getResultSet()) {

                            // 获取列信息
                            int columnCount = resultSet.getMetaData().getColumnCount();
                            columns.clear();
                            for (int i = 1; i <= columnCount; i++) {
                                columns.add(resultSet.getMetaData().getColumnLabel(i));
                            }

                            // 获取数据
                            resultData.clear();
                            while (resultSet.next()) {
                                List<Object> row = new ArrayList<>();
                                for (int i = 1; i <= columnCount; i++) {
                                    row.add(resultSet.getObject(i));
                                }
                                resultData.add(row);
                            }
                        }
                    }

                    logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("sql执行成功 \n");
                    workInstance = updateInstance(workInstance, logBuilder);
                }
            }

            // 保存运行结果
            if (!resultData.isEmpty()) {
                List<List<Object>> finalResult = new ArrayList<>();
                finalResult.add(new ArrayList<>(columns));
                finalResult.addAll(resultData);
                workInstance.setResultData(com.alibaba.fastjson.JSON.toJSONString(finalResult));
            }

            logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("Impala查询执行成功, 查看运行结果 \n");
            updateInstance(workInstance, logBuilder);

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "Impala查询执行异常 : "
                + e.getMessage().replace("<EOL>", "\n") + "\n");
        }
    }

    @Override
    protected void abort(WorkInstanceEntity workInstance) {

        Thread thread = WORK_THREAD.get(workInstance.getId());
        thread.interrupt();
    }
}
