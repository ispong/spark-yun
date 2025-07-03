package com.isxcode.star.modules.datasource.source.impl;

import com.isxcode.star.api.datasource.constants.DatasourceDriver;
import com.isxcode.star.api.datasource.constants.DatasourceType;
import com.isxcode.star.api.datasource.dto.ConnectInfo;
import com.isxcode.star.api.datasource.dto.QueryColumnDto;
import com.isxcode.star.api.datasource.dto.QueryTableDto;
import com.isxcode.star.api.work.res.GetDataSourceDataRes;
import com.isxcode.star.api.model.ao.DataModelColumnAo;
import com.isxcode.star.modules.model.entity.DataModelEntity;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.backend.api.base.properties.IsxAppProperties;
import com.isxcode.star.common.utils.aes.AesUtils;
import com.isxcode.star.modules.datasource.service.DatabaseDriverService;
import com.isxcode.star.modules.datasource.source.Datasource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ImpalaService extends Datasource {

    public ImpalaService(DatabaseDriverService dataDriverService, IsxAppProperties isxAppProperties,
        AesUtils aesUtils) {
        super(dataDriverService, isxAppProperties, aesUtils);
    }

    @Override
    public String getDataSourceType() {
        return DatasourceType.IMPALA;
    }

    @Override
    public String getDriverName() {
        return DatasourceDriver.IMPALA_DRIVER;
    }

    @Override
    public List<QueryTableDto> queryTable(ConnectInfo connectInfo) throws IsxAppException {

        List<QueryTableDto> result = new ArrayList<>();

        try (Connection connection = getConnection(connectInfo)) {

            DatabaseMetaData metaData = connection.getMetaData();

            // 获取数据库名称
            String databaseName = connection.getCatalog();
            if (databaseName == null || databaseName.isEmpty()) {
                // 从URL中提取数据库名称
                String url = connectInfo.getJdbcUrl();
                if (url.contains("/") && url.lastIndexOf("/") < url.length() - 1) {
                    databaseName = url.substring(url.lastIndexOf("/") + 1);
                    if (databaseName.contains("?")) {
                        databaseName = databaseName.substring(0, databaseName.indexOf("?"));
                    }
                }
            }

            // 查询表信息
            try (ResultSet tables = metaData.getTables(databaseName, null, "%", new String[] {"TABLE", "VIEW"})) {
                while (tables.next()) {
                    QueryTableDto queryTableDto = QueryTableDto.builder().tableName(tables.getString("TABLE_NAME"))
                        .tableComment(tables.getString("REMARKS")).build();
                    result.add(queryTableDto);
                }
            }

        } catch (SQLException e) {
            log.error("查询Impala表信息失败", e);
            throw new IsxAppException("查询表信息失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<QueryColumnDto> queryColumn(ConnectInfo connectInfo) throws IsxAppException {

        List<QueryColumnDto> result = new ArrayList<>();

        try (Connection connection = getConnection(connectInfo)) {

            DatabaseMetaData metaData = connection.getMetaData();

            // 获取数据库名称
            String databaseName = connection.getCatalog();
            if (databaseName == null || databaseName.isEmpty()) {
                // 从URL中提取数据库名称
                String url = connectInfo.getJdbcUrl();
                if (url.contains("/") && url.lastIndexOf("/") < url.length() - 1) {
                    databaseName = url.substring(url.lastIndexOf("/") + 1);
                    if (databaseName.contains("?")) {
                        databaseName = databaseName.substring(0, databaseName.indexOf("?"));
                    }
                }
            }

            // 查询列信息
            try (ResultSet columns = metaData.getColumns(databaseName, null, connectInfo.getTableName(), "%")) {
                while (columns.next()) {
                    QueryColumnDto queryColumnDto = QueryColumnDto.builder()
                        .columnName(columns.getString("COLUMN_NAME")).columnType(columns.getString("TYPE_NAME"))
                        .columnComment(columns.getString("REMARKS")).build();
                    result.add(queryColumnDto);
                }
            }

        } catch (SQLException e) {
            log.error("查询Impala列信息失败", e);
            throw new IsxAppException("查询列信息失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Long getTableTotalSize(ConnectInfo connectInfo) throws IsxAppException {

        try (Connection connection = getConnection(connectInfo)) {

            String sql = "SELECT COUNT(*) FROM " + connectInfo.getTableName();

            try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }

        } catch (SQLException e) {
            log.error("获取Impala表总数失败", e);
            throw new IsxAppException("获取表总数失败: " + e.getMessage());
        }

        return 0L;
    }

    public Long getTableColumnSize(ConnectInfo connectInfo) throws IsxAppException {

        try (Connection connection = getConnection(connectInfo)) {

            DatabaseMetaData metaData = connection.getMetaData();

            // 获取数据库名称
            String databaseName = connection.getCatalog();
            if (databaseName == null || databaseName.isEmpty()) {
                // 从URL中提取数据库名称
                String url = connectInfo.getJdbcUrl();
                if (url.contains("/") && url.lastIndexOf("/") < url.length() - 1) {
                    databaseName = url.substring(url.lastIndexOf("/") + 1);
                    if (databaseName.contains("?")) {
                        databaseName = databaseName.substring(0, databaseName.indexOf("?"));
                    }
                }
            }

            long columnCount = 0;
            try (ResultSet columns = metaData.getColumns(databaseName, null, connectInfo.getTableName(), "%")) {
                while (columns.next()) {
                    columnCount++;
                }
            }

            return columnCount;

        } catch (SQLException e) {
            log.error("获取Impala表列数失败", e);
            throw new IsxAppException("获取表列数失败: " + e.getMessage());
        }
    }

    @Override
    public void refreshTableInfo(ConnectInfo connectInfo) throws IsxAppException {
        // Impala通常不需要刷新表信息，因为它会自动检测Hive metastore的变化
        // 如果需要强制刷新，可以执行INVALIDATE METADATA命令
        try (Connection connection = getConnection(connectInfo)) {

            String sql = "INVALIDATE METADATA " + connectInfo.getTableName();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.execute();
            }

        } catch (SQLException e) {
            log.error("刷新Impala表信息失败", e);
            throw new IsxAppException("刷新表信息失败: " + e.getMessage());
        }
    }

    public String genDefaultSql(String tableName) {
        return "SELECT * FROM " + tableName + " LIMIT 100";
    }

    @Override
    public String generateDataModelSql(ConnectInfo connectInfo, List<DataModelColumnAo> modelColumnList,
        DataModelEntity dataModelEntity) throws IsxAppException {
        throw new IsxAppException("Impala数据源暂不支持数据模型功能");
    }

    @Override
    public Long getTableTotalRows(ConnectInfo connectInfo) throws IsxAppException {
        return getTableTotalSize(connectInfo);
    }

    @Override
    public Long getTableColumnCount(ConnectInfo connectInfo) throws IsxAppException {
        return getTableColumnSize(connectInfo);
    }

    @Override
    public String getPageSql(String sql) throws IsxAppException {
        // Impala支持LIMIT语法进行分页
        return sql + " LIMIT 1000";
    }

    @Override
    public GetDataSourceDataRes getTableData(ConnectInfo connectInfo) throws IsxAppException {

        String sql = "SELECT * FROM " + connectInfo.getTableName() + " LIMIT 1000";

        try (Connection connection = getConnection(connectInfo)) {

            try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

                // 获取列信息
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnLabel(i));
                }

                // 获取数据
                List<List<String>> data = new ArrayList<>();
                while (resultSet.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = resultSet.getObject(i);
                        row.add(value != null ? value.toString() : null);
                    }
                    data.add(row);
                }

                return GetDataSourceDataRes.builder().columns(columns).rows(data).build();

            }

        } catch (SQLException e) {
            log.error("获取Impala表数据失败", e);
            throw new IsxAppException("获取表数据失败: " + e.getMessage());
        }
    }
}
