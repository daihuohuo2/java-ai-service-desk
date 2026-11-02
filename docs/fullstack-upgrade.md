# 2026-11-02 全栈扩展交付说明

本次围绕现有业务增加可以亲手调试的功能。默认启动仍不依赖 Redis 或 RabbitMQ，需要观察这些组件时再启用。保留 Java 21、Spring Boot 4.1.2、Spring AI 2.0.2，业务版本仍为 1.0.0。

## 学习入口

1. [全栈学习路线](learning/全栈学习路线.md)：每阶段目标、请求流向、验收问题。
2. [文件阅读顺序](learning/文件阅读顺序.md)：按 Java、前端、SQL、测试、部署逐个文件说明。
3. [实验运行说明](learning/实验运行说明.md)：运行 Java、SQL、中间件和浏览器实验。
4. [进阶原理与运维](learning/进阶原理与运维.md)：事务、JVM、网络、Linux、HTTPS、后续微服务练习。

先运行 `java learning/FullstackBasics.java`，再阅读 `OrderController.java → OrderService.java → OrderMapper.java → OrderResponse.java`。旧 `OrderLesson.java` 仍保留，早期教程已标注历史版本。

## 已实现的业务能力

| 板块 | 实现 | 主要文件 |
|---|---|---|
| 订单 | typed record、MyBatis、本人范围、筛选、分页、校验 | `OrderController/Service/Mapper/Response`、`PageResponse` |
| 商品 | 分层、事务、版本化 Redis 缓存、失败回源 | `Product*`、`CatalogCache`、`RedisCatalogCache` |
| 工单 | 服务端筛选分页、CSV 导出、权限复用、公式注入防护 | `TicketQueryController/Service`、`CsvExport` |
| 知识索引 | 异步提交、SQL 持久化、幂等键、租约、失败重试、管理界面 | `IndexJob*`、`IndexQueueConfiguration`、`components/IndexJobs.tsx` |
| 前端 | React Router、登录上下文、页面拆分、URL 分页参数、表单反馈 | `App.tsx`、`hooks/`、`pages/`、`components/` |
| 数据库 | PostgreSQL/H2 与 MySQL 两套 Flyway 路径、索引、外键 | `db/migration/`、`db/migration-mysql/` |
| 工程化 | 真实中间件测试、Playwright、CI、监控和部署 | `compose*.yaml`、`deploy/`、`.github/workflows/ci.yml` |

原订单列表、同步索引、工单详情等接口保留兼容。AI 权限检查、引用校验和售后确认流程继续执行。

## 可选启动

先按 README 构建并初始化 `.env`。已有 Ollama 服务则复用 `OLLAMA_URL`，需要项目管理模型时增加 `-ManagedModels`。

```powershell
.\scripts\start.ps1 -Middleware -Observability
# 使用独立 MySQL 实例和数据卷
.\scripts\start.ps1 -MySql -Middleware
# MySQL 与监控一起用时，明确两个 Spring profile
$env:APP_PROFILES='mysql,observability'
.\scripts\start.ps1 -MySql -Middleware -Observability
```

普通入口为 `http://127.0.0.1:19082`；独立 MySQL 默认 `19083`；Prometheus 为 `http://127.0.0.1:19090`。两个带监控的环境不能同时占用同一宿主监控端口。修改端口时同步修改 `APP_ORIGIN`（MySQL 环境使用独立的 `MYSQL_APP_ORIGIN`）。

`compose.test-services.yaml` 是独立测试环境，固定的 `test-only-*` 密码仅用于本机测试。部署 Redis/RabbitMQ 不映射宿主端口，密码由环境提供，未设置专用密码时复用 `DATABASE_PASSWORD`。

## 边界

- Redis 只缓存商品列表。每次先读 SQL 商品版本号，不同版本使用不同缓存键，旧键 30 秒过期；事务内绕过共享缓存。没有实现共享会话或分布式登录/AI 限流。
- 任务采用至少一次投递：数据库保存任务，调度器反复发布可执行任务，消费者用 SQL 条件更新抢占、唯一租约 token 防止过期执行者覆盖新状态。重复消费被拒绝，失败最多 3 次，管理员可重试。不是“恰好一次”保证。
- 进度在一次索引尝试结束时记录；运行中显示状态和尝试次数，不承诺逐片段实时进度。不可取消的模型调用仍可能重复计算，嵌入写入为幂等更新。
- RabbitMQ 暂不可用时任务留在数据库等待；Redis 不可用时回源。启用相应组件后，其故障可能让健康检查显示 DOWN，业务回退与运维告警是不同层面的行为。
- MySQL 配置创建新环境，不自动转换 PostgreSQL 旧数据。原备份/恢复脚本仅支持 PostgreSQL，MySQL 备份恢复需要另行验证。
- Prometheus 提供采集和规则，没有配置外发通知。管理端口不映射宿主，Nginx 只代理健康检查、不代理 metrics。HTTPS 文件需真实域名与证书，尚未验收公网 TLS。
- 保留部分 `Map<String,Object>` 代码，订单/商品为类型化示例。未实现 Spring Cloud/Nacos、分布式事务、支付、短信、ES 或向量数据库。
- JVM/网络/操作系统/Linux/Git 提供原理和实验入口，不表示已有生产实践。Vue 是可选替代路线，本项目继续使用 React。

## 验收

实际结果见 [summary.json](verification/fullstack-20261102/summary.json)，CI 见 [GitHub Actions](https://github.com/daihuohuo2/java-ai-service-desk/actions/workflows/ci.yml)。基础业务在三个数据库上重复验证，不把重复运行累计成不同用例数。真实中间件与真实模型分别列证据。

本机隔离部署与原演示数据卷分开，未验收指定服务器或生产负载。归档不包含 `.env`、数据库、模型权重或运行日志。
