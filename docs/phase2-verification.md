# 第二阶段实现与验收记录

日期：2026-10-13（Asia/Shanghai）。实现范围为数据库设计、Flyway 初始化和后端基础数据访问，无页面或交易业务。

## 1. 新增文件

以下路径相对于项目根目录（按本次执行前的文件存在情况判断；Git 尚无首次提交，不能将全部 untracked 文件视为本次新增）：

- backend/src/main/resources/db/migration/V1__init_database.sql
- docs/database-design.md
- docs/er-diagram.md
- docs/phase2-verification.md（本文）
- backend/src/main/java/com/example/market/entity/User.java
- backend/src/main/java/com/example/market/mapper/UserMapper.java
- backend/src/main/java/com/example/market/common/DatabaseMigrationConfiguration.java
- backend/src/main/java/com/example/market/entity/package-info.java
- backend/src/main/java/com/example/market/mapper/package-info.java
- backend/src/main/java/com/example/market/service/package-info.java
- backend/src/main/java/com/example/market/controller/package-info.java
- backend/src/main/java/com/example/market/dto/package-info.java
- backend/src/main/java/com/example/market/vo/package-info.java
- backend/src/main/java/com/example/market/common/package-info.java
- backend/src/test/java/com/example/market/common/DatabaseMigrationConfigurationTests.java
- backend/src/test/java/com/example/market/DatabaseIntegrationTests.java

## 2. 修改文件

- backend/pom.xml：添加 MyBatis-Plus 3.5.17 Boot3 starter、Flyway core 和 PostgreSQL 模块。Spring Boot 版本保持3.5.7，已有依赖保留。
- backend/src/main/resources/application.yml：目标库默认改为 secondhand_market，添加安全 Flyway 和 MyBatis-Plus 配置，既有 JPA/服务端口配置保留。
- backend/src/main/java/com/example/secondhandmarket/SecondhandMarketApplication.java：扫描新增业务包与 Mapper，保留原启动类。
- backend/src/test/java/com/example/secondhandmarket/SecondhandMarketApplicationTests.java：无需密码的上下文与 UserMapper 装配测试，明确不代表实库查询验证。
- .env.example：更新目标库示例，无真实密码。
- scripts/start-backend.ps1：添加 -VerifyDatabase 选项，隐藏输入密码后运行真实集成测试，成功后启动；保留项目JDK选择和原密码清理逻辑。
- README.md：补充第二阶段结构、建库流程和验证命令。

## 3. 表结构总结

| 表 | 功能 |
|---|---|
| users | 用户身份、BCrypt哈希、角色与账号状态 |
| categories | 分类及可空父级 |
| products | 商品、卖家、分类、NUMERIC价格和审核/交易状态 |
| product_images | 商品多张图片及排序 |
| favorites | 收藏，用户+商品唯一 |
| orders | 买卖订单、唯一订单号、标题和价格快照 |
| order_events | 订单状态变更记录 |
| conversations | 商品买卖双方会话 |
| messages | 会话文本消息及已读标记 |
| reports | 商品举报及处理状态 |
| payment_records | 模拟支付流水及金额 |
| audit_logs | 管理员操作留痕 |

全部表包含 BIGINT identity 主键、created_at/updated_at TIMESTAMPTZ NOT NULL；数据库触发器维护更新时间。金额为 NUMERIC(12,2)。无预置业务数据。

## 4. ER关系总结

分类自关联，分类对应多商品；用户发布商品并关联收藏、买卖订单、会话、消息、举报和日志。商品对应图片、收藏、订单、会话和举报；订单对应事件与支付记录；会话对应消息。订单/会话复合外键保证商品卖家匹配。所有历史外键采用 RESTRICT。具体字段和索引见 database-design.md；图见 er-diagram.md。

## 5. Flyway执行结果

2026-10-13 11:10（Asia/Shanghai），使用用户确认的正确连接凭据，Spring Boot真实执行Flyway迁移。PostgreSQL16.11连接成功；V1__init_database.sql成功执行，schema版本为1。

只读核查flyway_schema_history：version=1，description=init database，success=true。public中共13张表：12张业务表和1张Flyway历史表。users当前0行，未插入虚构账户或业务数据。

随后11:11完整HTTP服务启动时再次校验迁移成功，显示Schema public is up to date，未重复建表或覆盖数据。禁用clean和自动baseline配置保持启用。

## 6. Spring Boot启动结果

项目Temurin JDK17.0.20.1，Spring Boot3.5.7，启动成功。Tomcat监听18080；启动日志为Started SecondhandMarketApplication。当前本次启动的后端保持运行。

| 接口 | HTTP状态 | 响应 |
|---|---|---|
| http://localhost:18080/api/health | 200 | {"status":"ok"} |
| http://localhost:18080/api/health/database | 200 | {"status":"ok"} |

后一个接口实际通过JdbcTemplate执行SELECT 1。

## 7. 测试结果

在项目JDK17及Maven Wrapper下运行mvnw.cmd -B -ntp test，并设置RUN_DB_TESTS=true：BUILD SUCCESS。

- 共4项：4通过、0失败、0错误、0跳过。
- 2项维护库URL安全检查通过。
- 1项Spring上下文及UserMapper装配通过。
- 1项真实PostgreSQL集成测试通过：数据库名、Flyway版本、12张表、UserMapper.selectCount与JDBC计数一致，以及UserMapper.selectList查询成功。
- 此前V1真实PostgreSQL事务结构验证通过并回滚，12张表和24个非空TIMESTAMPTZ字段均确认。
- PowerShell启动脚本语法检查通过。

此前一次实库测试因用户提供的旧凭据返回28P01，用户确认正确凭据后已解除该阻碍。没有重置数据库密码、更改认证配置、修改Docker环境或删除已有数据。凭据只经临时内存管道传入进程环境变量，不写入文件、代码或命令参数。

## 8. 当前完成度

第一阶段环境初始化完成。第二阶段数据库设计、Flyway迁移、后端基础数据访问及本阶段验收均已完成。仍未开发注册、登录、JWT、权限、商品/订单/聊天业务或任何页面。

## 9. 下一阶段建议

先确定统一响应、全局异常处理和参数校验规范，再按用户确认的顺序逐项开发后端业务接口；不要一次生成整个系统，不提前开发页面。

Navicat展开secondhand_market下的public/表并刷新即可查看业务表；当前运行的后端可直接访问上述健康检查接口。后续自行启动（避免与当前运行实例同时占用18080）可使用原脚本隐藏输入密码：

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File "K:\graduate work\secondhand-market\scripts\start-backend.ps1" -VerifyDatabase
```