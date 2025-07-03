# Impala 查询作业实现文档

## 概述

本文档描述了为 Spark-Yun 系统添加 Impala 查询作业支持的实现过程。该实现参考了现有的 `ApiExecutor` 和其他数据库执行器的设计模式。

## 实现的功能

1. **新增 Impala 查询作业类型** - 支持通过 Impala 执行 SQL 查询
2. **Impala 数据源支持** - 添加 Impala 作为新的数据源类型
3. **前端界面集成** - 在前端界面中添加 Impala 相关选项

## 修改的文件

### 后端常量定义

1. **WorkType.java** - 添加新的工作类型常量
   ```java
   String QUERY_IMPALA = "QUERY_IMPALA";
   ```

2. **DatasourceType.java** - 添加 Impala 数据源类型
   ```java
   String IMPALA = "IMPALA";
   ```

3. **DatasourceDriver.java** - 添加 Impala JDBC 驱动
   ```java
   String IMPALA_DRIVER = "com.cloudera.impala.jdbc.Driver";
   ```

### 核心实现类

4. **ImpalaExecutor.java** - 新建 Impala 查询执行器
   - 继承 `WorkExecutor` 抽象类
   - 实现 Impala SQL 查询逻辑
   - 支持多条 SQL 语句执行
   - 支持变量和函数解析
   - 返回查询结果数据

5. **ImpalaService.java** - 新建 Impala 数据源服务
   - 继承 `Datasource` 抽象类
   - 实现表和列信息查询
   - 支持数据预览功能
   - 实现表统计信息获取

### 配置更新

6. **WorkConfigService.java** - 更新工作配置服务
   - 为 Impala 查询作业添加默认 SQL 脚本生成

7. **PrqlExecutor.java** - 更新数据库类型转换
   - 添加 Impala 到 PRQL 的数据库类型映射

### 前端配置

8. **workflow.config.ts** - 添加 Impala 查询作业选项
9. **datasource.config.ts** - 添加 Impala 数据源类型选项
10. **其他前端配置文件** - 在各个相关配置文件中添加 Impala 支持

## 核心特性

### ImpalaExecutor 主要功能

- **环境检测**: 验证数据源配置和脚本有效性
- **SQL 解析**: 支持变量替换、函数解析和注释移除
- **多语句执行**: 支持分号分隔的多条 SQL 语句
- **结果处理**: 返回查询结果的列信息和数据
- **错误处理**: 提供详细的错误信息和日志

### ImpalaService 主要功能

- **连接管理**: 使用 Impala JDBC 驱动建立数据库连接
- **元数据查询**: 获取数据库表和列信息
- **数据预览**: 支持表数据的分页查询
- **统计信息**: 获取表的行数和列数统计
- **元数据刷新**: 支持 `INVALIDATE METADATA` 命令

## 使用方法

1. **配置 Impala 数据源**
   - 在数据源管理中选择 "Impala" 类型
   - 配置 JDBC URL: `jdbc:impala://host:port/database`
   - 设置用户名和密码

2. **创建 Impala 查询作业**
   - 在工作流中选择 "Impala查询作业" 类型
   - 选择已配置的 Impala 数据源
   - 编写 SQL 查询语句

3. **执行和监控**
   - 运行作业并查看执行日志
   - 查看查询结果数据
   - 监控作业执行状态

## 技术细节

### 依赖要求

- Impala JDBC 驱动 (com.cloudera.impala.jdbc.Driver)
- 需要在系统中上传对应的 Impala JDBC 驱动 JAR 文件

### 支持的 SQL 功能

- 标准 SELECT 查询
- 多表 JOIN 操作
- 聚合函数和分组
- 子查询和 CTE
- Impala 特有的函数和语法

### 限制和注意事项

- 数据模型功能暂不支持 Impala
- 需要确保 Impala 集群的网络连通性
- 大结果集查询会被限制在 1000 行以内

## 测试验证

实现已通过编译测试，确保：
- 所有新增代码能够正常编译
- 与现有系统架构兼容
- 遵循项目的代码规范和设计模式

## 后续扩展

可以考虑的后续改进：
1. 添加 Impala 特有的优化配置
2. 支持 Impala 的分区表查询
3. 集成 Impala 的查询计划分析
4. 添加更多的 Impala 特有功能支持
