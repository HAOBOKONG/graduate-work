# 开发环境检查结果

检查日期：2026-10-13

| 组件 | 验证结果 |
| --- | --- |
| Docker Desktop | 4.53.0，Docker Engine 29.0.1，运行正常 |
| PostgreSQL | Docker 容器 `post-demo`，PostgreSQL 16.11，端口 `5432:5432`，连接正常 |
| Java | 项目使用 Eclipse Temurin JDK 17.0.20.1 |
| Maven | 系统 Maven 3.9.6；后端提供 Maven Wrapper |
| Node.js | 24.11.1，x64 |
| npm | 11.6.2 |
| Git | 2.52.0.windows.1 |

数据库密码仅通过本地环境变量 `DB_PASSWORD` 提供，不写入代码或 Git 仓库。

## 工程验证

- Maven Wrapper 测试：通过，1 个测试，0 个失败。
- Spring Boot：使用 Java 17.0.20.1 启动成功，`/api/health` 返回 `{"status":"ok"}`。
- PostgreSQL JDBC：用户在本机输入密码后，确认 `/api/health/database` 返回成功；Spring Boot 到 Docker PostgreSQL 的连接已验证。
- Vue 前端：生产构建成功，Vite 开发服务器返回 HTTP 200。
- 本机端口 `8080` 被其他程序占用，后端默认使用 `18080`。
