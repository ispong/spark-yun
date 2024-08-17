package com.isxcode.star.modules.datasource.source.impl;

import com.isxcode.star.api.datasource.constants.DatasourceDriver;
import com.isxcode.star.api.datasource.constants.DatasourceType;
import com.isxcode.star.modules.datasource.entity.DatasourceEntity;
import com.isxcode.star.modules.datasource.source.SourceService;

import java.util.Collections;
import java.util.List;

public class ClickhouseService implements SourceService {

    @Override
    public String getDataSourceType() {
        return DatasourceType.CLICKHOUSE;
    }

    @Override
    public String getDriverName() {
        return DatasourceDriver.CLICKHOUSE_DRIVER;
    }

    @Override
    public List<String> queryTables(DatasourceEntity datasourceEntity) {
        return Collections.emptyList();
    }
}
