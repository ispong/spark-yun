package com.isxcode.star.modules.datasource.service;

import com.isxcode.star.api.datasource.constants.ColumnType;
import com.isxcode.star.api.datasource.constants.DatasourceType;
import com.isxcode.star.api.datasource.pojos.dto.KafkaConfig;
import com.isxcode.star.api.datasource.pojos.dto.SecurityColumnDto;
import com.isxcode.star.api.work.exceptions.WorkRunException;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.backend.api.base.properties.IsxAppProperties;
import com.isxcode.star.common.utils.AesUtils;
import com.isxcode.star.modules.datasource.entity.DatasourceEntity;
import com.isxcode.star.modules.datasource.repository.DatasourceRepository;
import com.isxcode.star.modules.datasource.source.DataSourceFactory;
import com.isxcode.star.modules.datasource.source.Datasource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.springframework.stereotype.Service;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.SqlNode;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
@RequiredArgsConstructor
public class DatasourceService {

    private final IsxAppProperties isxAppProperties;

    private final DatasourceRepository datasourceRepository;

    private final DatabaseDriverService dataDriverService;

    private final DataSourceFactory dataSourceFactory;

    /**
     * 所有的驱动. driverId driver
     */
    public final static Map<String, DriverShim> ALL_EXIST_DRIVER = new ConcurrentHashMap<>();

    private final AesUtils aesUtils;

    public String getDriverClass(String datasourceType) {

        Datasource factoryDatasource = dataSourceFactory.getDatasource(datasourceType);
        return factoryDatasource.getDriverName();
    }

    public DatasourceEntity getDatasource(String datasourceId) {

        return datasourceRepository.findById(datasourceId).orElseThrow(() -> new IsxAppException("数据源不存在"));
    }

    public String getDatasourceName(String datasourceId) {

        DatasourceEntity datasource = datasourceRepository.findById(datasourceId).orElse(null);
        return datasource == null ? datasourceId : datasource.getName();
    }

    public void executeSql(DatasourceEntity datasource, String sql) {

        Datasource datasource1 = dataSourceFactory.getDatasource(datasource.getId());
        try (Connection connection = datasource1.getConnection(datasource);
            Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (e.getErrorCode() == 1364) {
                throw new IsxAppException("表不存在");
            }
            throw new IsxAppException("提交失败");
        }
    }

    public void securityExecuteSql(String datasourceId, String securityExecuteSql,
        List<SecurityColumnDto> securityColumns) {

        DatasourceEntity datasource = this.getDatasource(datasourceId);

        Datasource datasource1 = dataSourceFactory.getDatasource(datasource.getId());
        try (Connection connection = datasource1.getConnection(datasource);
            PreparedStatement statement = connection.prepareStatement(securityExecuteSql);) {
            for (int i = 0; i < securityColumns.size(); i++) {
                this.transAndSetParameter(statement, securityColumns.get(i), i);
            }
            statement.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException("提交失败");
        }
    }

    public ResultSet securityQuerySql(String datasourceId, String securityExecuteSql,
        List<SecurityColumnDto> securityColumns) throws SQLException {

        DatasourceEntity datasource = this.getDatasource(datasourceId);

        Datasource datasource1 = dataSourceFactory.getDatasource(datasource.getDbType());
        Connection connection = datasource1.getConnection(datasource);
        PreparedStatement statement = connection.prepareStatement(securityExecuteSql);
        for (int i = 0; i < securityColumns.size(); i++) {
            this.transAndSetParameter(statement, securityColumns.get(i), i);
        }
        return statement.executeQuery();
    }

    public long securityGetTableCount(String datasourceId, String securityExecuteSql,
        List<SecurityColumnDto> securityColumns) {

        DatasourceEntity datasource = this.getDatasource(datasourceId);

        Datasource datasource1 = dataSourceFactory.getDatasource(datasource.getDbType());
        try (Connection connection = datasource1.getConnection(datasource);
            PreparedStatement statement = connection.prepareStatement(securityExecuteSql);) {
            for (int i = 0; i < securityColumns.size(); i++) {
                this.transAndSetParameter(statement, securityColumns.get(i), i);
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException("提交失败");
        }
        throw new IsxAppException("查询总条数异常");
    }

    public boolean tableIsExist(DatasourceEntity datasource, String tableName) {

        Datasource datasource1 = dataSourceFactory.getDatasource(datasource.getDbType());
        try (Connection connection = datasource1.getConnection(datasource);
            PreparedStatement preparedStatement =
                connection.prepareStatement("SELECT 1 FROM " + tableName + " WHERE 1 = 0")) {
            preparedStatement.executeQuery();
            return true;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public void transAndSetParameter(PreparedStatement statement, SecurityColumnDto securityColumnDto,
        int parameterIndex) throws SQLException {

        switch (securityColumnDto.getType()) {
            case ColumnType.STRING:
                if (securityColumnDto.getValue() == null) {
                    statement.setNull(parameterIndex + 1, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(parameterIndex + 1, String.valueOf(securityColumnDto.getValue()));
                }
                break;
            case ColumnType.INT:
                if (securityColumnDto.getValue() == null) {
                    statement.setNull(parameterIndex + 1, Types.INTEGER);
                } else {
                    statement.setInt(parameterIndex + 1,
                        Integer.parseInt(String.valueOf(securityColumnDto.getValue())));
                }
                break;
            case ColumnType.DOUBLE:
                if (securityColumnDto.getValue() == null) {
                    statement.setNull(parameterIndex + 1, Types.DOUBLE);

                } else {
                    statement.setDouble(parameterIndex + 1,
                        Double.parseDouble(String.valueOf(securityColumnDto.getValue())));
                }
                break;
            case ColumnType.TIMESTAMP:
            case ColumnType.DATE:
            case ColumnType.DATE_TIME:
                if (securityColumnDto.getValue() == null) {
                    statement.setNull(parameterIndex + 1, Types.TIMESTAMP);
                } else {
                    statement.setTimestamp(parameterIndex + 1,
                        new Timestamp(Long.parseLong(String.valueOf(securityColumnDto.getValue()))));
                }
                break;
            case ColumnType.BIG_DECIMAL:
                if (securityColumnDto.getValue() == null) {
                    statement.setNull(parameterIndex + 1, Types.NUMERIC);
                } else {
                    statement.setBigDecimal(parameterIndex + 1,
                        new BigDecimal(String.valueOf(securityColumnDto.getValue())));
                }
                break;
            case ColumnType.BOOLEAN:
                if (securityColumnDto.getValue() == null) {
                    statement.setNull(parameterIndex + 1, Types.BOOLEAN);
                } else {
                    statement.setBoolean(parameterIndex + 1,
                        Boolean.parseBoolean(String.valueOf(securityColumnDto.getValue())));
                }
                break;
            default:
                throw new IsxAppException("字段类型不支持");
        }
    }

    public String parseDbName(String jdbcUrl) {
        Pattern pattern = Pattern.compile("jdbc:\\w+://\\S+/(\\w+)");
        Matcher matcher = pattern.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "default";
    }

    /**
     * 解析sql select * from table where a = '${value1}' and b = ${value2} 获取sql中的参数顺序 List
     * [value1,value2]
     */
    public List<SecurityColumnDto> transSecurityColumns(String sql) {

        List<SecurityColumnDto> securityColumnList = new ArrayList<>();

        // 使用正则截取${}中的字符
        String pattern = "'\\$\\{(?!UPDATE_COLUMN\\b)([^}]+)\\}'";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(sql);
        int columnIndex = 10;
        while (matcher.find()) {
            String name = matcher.group(1);
            securityColumnList.add(SecurityColumnDto.builder().name(columnIndex + "." + name).build());
            columnIndex++;
        }
        return securityColumnList;
    }

    /**
     * 解析sql，将 select * from table where a = '${value1}' and b = ${value2} 转成 select * from table where
     * a = ? and b = ?
     */
    public String transSecuritySql(String sql) {

        return sql.replaceAll("'\\$\\{(?!UPDATE_COLUMN\\b)([^}]+)\\}'", "?");
    }

    public boolean isQueryStatement(String sql) {

        SqlParser parser = SqlParser.create(sql);
        try {

            String sqlUpper = sql.trim().toUpperCase();
            if (sqlUpper.startsWith("SHOW TABLES") || sqlUpper.startsWith("SHOW DATABASES")) {
                return true;
            }

            SqlNode sqlNode = parser.parseQuery();
            return sqlNode.getKind() == SqlKind.SELECT || sqlNode.getKind() == SqlKind.ORDER_BY;
        } catch (SqlParseException e) {
            log.error(e.getMessage(), e);
            throw new WorkRunException(e.getMessage());
        }
    }

    public boolean checkSqlValid(String sql) {

        SqlParser parser = SqlParser.create(sql);
        try {
            parser.parseQuery(sql);
            return true;
        } catch (SqlParseException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public String genDefaultSql(String datasourceId) {

        if (StringUtils.isEmpty(datasourceId)) {
            return "show databases";
        }

        DatasourceEntity datasource = getDatasource(datasourceId);

        switch (datasource.getDbType()) {
            case DatasourceType.HANA_SAP:
                return "SELECT TABLE_NAME FROM SYS.TABLES;";
            default:
                return "show databases";
        }
    }

    public void checkKafka(KafkaConfig kafkaConfig) throws ExecutionException, InterruptedException {

        Properties properties = new Properties();
        if (kafkaConfig.getProperties() != null) {
            properties.putAll(kafkaConfig.getProperties());
        }
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        properties.put(AdminClientConfig.RETRIES_CONFIG, 0);
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000);

        try (AdminClient adminClient = AdminClient.create(properties)) {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            Set<String> strings = listTopicsResult.names().get();
            if (!strings.contains(kafkaConfig.getTopic())) {
                throw new RuntimeException("topic不存在");
            }
        }
    }

    public Set<String> queryKafkaTopic(KafkaConfig kafkaConfig) throws ExecutionException, InterruptedException {

        Properties properties = new Properties();
        if (kafkaConfig.getProperties() != null) {
            properties.putAll(kafkaConfig.getProperties());
        }
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        properties.put(AdminClientConfig.RETRIES_CONFIG, 0);
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000);

        try (AdminClient adminClient = AdminClient.create(properties)) {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            return listTopicsResult.names().get();
        }
    }

    /**
     * 判断sql是否有limit限制.
     */
    public boolean hasLimit(String sql) {

        String sqlUpper = sql.trim().toUpperCase();
        if (sqlUpper.startsWith("SHOW TABLES") || sqlUpper.startsWith("SHOW DATABASES")) {
            return true;
        }

        SqlParser parser = SqlParser.create(sql);
        try {
            SqlNode sqlNode = parser.parseStmt();
            if (sqlNode instanceof SqlSelect) {
                SqlSelect select = (SqlSelect) sqlNode;
                return select.getFetch() != null;
            } else if (sqlNode instanceof SqlOrderBy) {
                SqlOrderBy orderBy = (SqlOrderBy) sqlNode;
                return orderBy.fetch != null;
            } else {
                return false;
            }
        } catch (SqlParseException e) {
            log.error(e.getMessage(), e);
            throw new WorkRunException(e.getMessage());
        }
    }

    /**
     * 判断sql是否有where.
     */
    public boolean hasWhere(String sql) {

        SqlParser parser = SqlParser.create(sql);
        try {
            SqlNode sqlNode = parser.parseStmt();
            if (sqlNode instanceof SqlSelect) {
                SqlSelect select = (SqlSelect) sqlNode;
                return select.getWhere() != null;
            } else {
                return false;
            }
        } catch (SqlParseException e) {
            log.error(e.getMessage(), e);
            throw new WorkRunException(e.getMessage());
        }
    }

    /**
     * 生成sql的limit sql.
     */
    public String getSqlLimitSql(String dbType, Boolean hasWhere) {

        switch (dbType) {
            case DatasourceType.ORACLE:
                return hasWhere ? " AND ROWNUM <= 200" : " WHERE ROWNUM <= 200";
            default:
                return " limit 200";
        }
    }
}
