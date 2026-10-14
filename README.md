# 基于 Spring Boot 和 Vue3 的二手商品交易平台

本科毕业设计基础工程。第二阶段新增数据库设计、Flyway 迁移及 MyBatis-Plus User 数据访问基础，不包含交易业务功能。

## 技术栈

- 后端：Java 17、Spring Boot 3.5.7、Maven Wrapper
- 前端：Vue 3、TypeScript、Vite、Element Plus
- 数据库：Docker 中的 PostgreSQL 16.11

## 目录结构

```text
secondhand-market/
├─ backend/       Spring Boot 后端
├─ frontend/      Vue 3 前端
├─ docs/          项目文档
├─ scripts/       Windows PowerShell 启动脚本
└─ README.md
```

## 数据库配置

后端从环境变量读取连接参数：

| 环境变量 | 默认值 |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/secondhand_market` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | 无默认密码 |

`.env.example` 只提供变量示例。真实密码不要写入该文件、代码、命令历史或 Git 仓库。

## 启动后端

在项目根目录打开 PowerShell：

```powershell
.\scripts\start-backend.ps1
```

如果当前窗口是 CMD，请使用 Windows PowerShell 运行脚本：

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\start-backend.ps1"
```

脚本会安全提示输入 PostgreSQL 密码，并使用 Maven Wrapper 启动服务。默认访问：

```text
http://localhost:18080/api/health
```

后端默认使用 `18080` 端口，启动时会在终端显示实际端口。如需指定其他端口，可在运行脚本前设置 `SERVER_PORT` 环境变量。普通健康检查的预期响应是：

```json
{"status":"ok"}
```

验证 Spring Boot 通过 JDBC 连接 PostgreSQL：

```text
http://localhost:<实际端口>/api/health/database
```

该接口只执行 `SELECT 1`。连接成功时响应 `{"status":"ok"}`，不会返回或记录数据库密码。

## 启动前端

另开一个 PowerShell 窗口：

```powershell
.\scripts\start-frontend.ps1
```

启动后访问终端显示的地址，默认是：

```text
http://localhost:5173
```

## 构建验证

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm.cmd run build
```

环境检查记录见 `docs/environment-check.md`。

## 第二阶段数据库与后端基础

数据库字段、约束和索引见 [数据库设计](docs/database-design.md)，关系见 [ER 图](docs/er-diagram.md)。
后端启动先安全检查/创建 `secondhand_market`，随后 Flyway 自动执行 V1。保留已有库和数据，遇到非空且未纳管 schema 时停止；不要使用 baseline 或 clean 绕过检查。
既有 JPA 配置保持 ddl-auto=none；新增数据访问使用 MyBatis-Plus。启动类保留原包名并扫描 com.example.market。
entity/mapper/service/controller/dto/vo/common 的职责由各包 package-info.java 说明。本阶段只有 User 和 UserMapper，无业务 Service、登录/JWT/API 或页面。

真实 PostgreSQL 验证（在本地会话安全设置 DB_PASSWORD 后）：

```powershell
$env:RUN_DB_TESTS = 'true'
cd backend
.\mvnw.cmd test
```

使用项目 JDK 17，避免系统 Java 8；Maven Wrapper 使用项目 .cache/maven。未设置 RUN_DB_TESTS 时真实数据库集成测试明确跳过，不以 Mock 替代。

也可在项目根目录执行下列命令，隐藏输入密码后先运行全部测试（含真实 PostgreSQL），通过后启动后端：

```powershell
.\scripts\start-backend.ps1 -VerifyDatabase
```

若当前会话的 DB_URL 仍指向旧的 postgres 库，请先移除旧值以使用新默认值，或将其设置为 jdbc:postgresql://localhost:5432/secondhand_market。
## 当前工作区路径

项目已迁移到 `K:\graduate work\secondhand-market`，现有专用JDK与配套缓存位于同级 `K:\graduate work\work`。启动脚本使用相对路径，仍自动选择该JDK17。

CMD启动后端（隐藏输入数据库密码）：

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File "K:\graduate work\secondhand-market\scripts\start-backend.ps1"
```

CMD启动前端：

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -File "K:\graduate work\secondhand-market\scripts\start-frontend.ps1"
```