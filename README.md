# 知序 · Java + AI 智能客服与售后工单系统

[![CI](https://github.com/daihuohuo2/java-ai-service-desk/actions/workflows/ci.yml/badge.svg)](https://github.com/daihuohuo2/java-ai-service-desk/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

本项目将商品知识问答、本人订单查询、AI 售后草稿、客户确认、人工工单处理和审计记录串成完整流程。Java 负责权限与业务决策，模型负责理解意图和选择资料。数据均为虚构，可用于本机演示与作品投递。

Java + Spring AI customer service desk with local Ollama models, grounded knowledge answers, order access control and a human-confirmed after-sales workflow.

![智能客服与原文引用](docs/verification/ui/chat-grounded.png)

## Java 全栈学习版（2026-11-03）

从 [学习入口](docs/learning/README.md) 开始。已补充分层包、实体/DTO、MyBatis-Plus、Lombok/MapStruct、服务接口与实现、分组与自定义校验、统一 JSON、Mockito 单测和 Maven 三模块。订单的执行代码现位于 `backend/desk-app/src/main/java/cn/liuyang/desk/service/impl/OrderServiceImpl.java`。

先看 [订单请求的逐句与变量讲解](docs/learning/01-订单请求与变量轨迹.md)，再按 [真实 Git 演进](docs/learning/05-Git演进与动手练习.md) 学习。可执行 JAR 路径是 `backend/desk-app/target/service-desk-1.0.0.jar`。本分支是本地升级；旧 CI 徽章和旧日期报告不代表此次改动已经在远程执行。

## 立即体验

完成下方构建和启动后，打开 **http://127.0.0.1:19082/**。这是本地入口，项目未提供公共在线演示服务；Docker Desktop 须处于运行状态。

| 身份 | 用户名 | 本机演示密码 | 能力 |
|---|---|---|---|
| 客户 | customer | DemoCustomer!2026 | 本人订单、知识问答、售后申请、工单回复 |
| 第二客户 | customer2 | DemoCustomer!2026 | 用于验证跨账号隔离 |
| 客服 | agent | DemoAgent!2026 | 工单接单、补充材料、解决、运营概览 |
| 管理员 | admin | DemoAdmin!2026 | 客服能力，以及知识、商品和审计管理 |

登录页点击角色即可填入对应演示账号。这些是公开的虚构演示账号，不能作为互联网环境的正式账户使用。

推荐演示：询问“栖光阅读灯需要充电吗？” → 展开原文引用 → 查询 `ZS20261030001` → 请求退货退款 → 核对草稿并确认 → 切换客服接单并解决 → 客户确认关闭。详细讲解见 [演示说明](docs/demo.md)。

## 技术实现

刚学 Java？从 [全栈学习路线](docs/learning/全栈学习路线.md) 开始，按 [文件阅读顺序](docs/learning/文件阅读顺序.md) 阅读。先运行 `learning/FullstackBasics.java`，再跟踪 `OrderController → OrderServiceImpl → OrderMapper → SQL`，最后进入 AI。

- Java 21 / Spring Boot 4.1.2 / Spring AI 2.0.2 / Spring Security / MyBatis-Plus + 类型化 JDBC / Flyway / MapStruct / Lombok。
- React 19 / TypeScript / Vite / React Router：分页面、登录上下文、表单校验、服务端分页筛选、CSV 导出及手机布局。
- PostgreSQL 17 默认容器部署；H2 轻量开发与测试；MySQL 8.4 独立配置与迁移脚本。
- 可选 Redis 商品缓存；SQL 持久化的知识索引任务、RabbitMQ 消费、租约与重试；Micrometer + Prometheus 监控配置。
- Ollama + Qwen3 4B Instruct + Qwen3 Embedding 0.6B，本地推理，不要求付费闭源 SDK 或云 API Key。
- 中文二元字符检索与向量余弦相似度混合排序；商品范围过滤，模型选择资料 ID，后端逐一校验引用并输出原文。
- Cookie 会话、CSRF、角色权限、订单/会话归属检查；工单行锁、版本号与幂等键；AI 草稿二次确认；错误与超时降级。
- SSE 提供处理阶段与校验后的完整结果，不直接向客户输出未经校验的模型 token。

## 构建与启动

构建需要 JDK 21、Node.js 24、Docker Compose v2，以及首次下载依赖的网络。项目自带 Maven Wrapper，固定 Maven 3.9.16。

```powershell
git clone https://github.com/daihuohuo2/java-ai-service-desk.git
cd java-ai-service-desk
.\scripts\build.ps1
.\scripts\start.ps1 -ManagedModels
```

默认使用独立模型卷。支持 NVIDIA GPU 的 Docker 环境可用以下命令加速；CPU 模式较慢，其性能尚未专门验收：

```powershell
.\scripts\start.ps1 -ManagedModels -Gpu
```

GitHub 仓库提供源码，需先构建。首次启动会自动创建 `.env` 并生成随机数据库密码，还需下载容器镜像及约 3.2 GB 模型权重。已有模型服务时可配置 `OLLAMA_URL` 并省略 `-ManagedModels`。Linux 步骤见 [部署说明](docs/deployment.md)。

只运行自动测试不需要 Docker、GPU 或下载模型：

```powershell
.\backend\mvnw.cmd -f backend/pom.xml verify
npm.cmd ci --prefix frontend
npm.cmd test --prefix frontend
npm.cmd run build --prefix frontend
```

GitHub Actions 同时验证 PostgreSQL、MySQL、真实 Redis/RabbitMQ、Java 学习实验和 Chromium 桌面/手机浏览器流程。CI 中模型为 Mock 或关闭；真实模型验收使用 [验收脚本](scripts/live-acceptance.mjs) 在部署后的演示环境中运行。

### 全栈扩展的可选启动方式

```powershell
.\scripts\start.ps1 -Middleware -Observability
# 或使用独立 MySQL 数据卷（不自动迁移现有 PostgreSQL 数据）
.\scripts\start.ps1 -MySql -Middleware
# 自动启动独立数据库/中间件测试环境并验证
.\scripts\run-integration.ps1
```

`-Middleware` 启用 Redis 和 RabbitMQ；不加时仍可使用默认数据库任务调度器。已有模型可复用 `OLLAMA_URL`，没有模型服务则增加 `-ManagedModels`。监控入口仅绑定本机 `19090`。完整说明见 [全栈扩展交付说明](docs/fullstack-upgrade.md) 与 [实验运行说明](docs/learning/实验运行说明.md)。

## 文档与验收证据

2026-11-02 全栈扩展的功能、复现方式和验收范围见 [交付说明](docs/fullstack-upgrade.md) 与 [本次验收摘要](docs/verification/fullstack-20261102/summary.json)。2026-10-31 的 [Spring Boot 升级报告](docs/upgrades/20261031-spring-boot-4.md) 和 2026-10-30 ZIP 验收属于历史记录。

历史本机交付 ZIP、数据库、模型、环境配置和机器诊断日志未上传。公开的验收摘要与虚构场景记录见 [证据说明](docs/verification/README.md)。

| 材料 | 路径 |
|---|---|
| 需求与图片要求对应 | [requirements.md](docs/requirements.md) |
| 架构、数据模型、状态机 | [architecture.md](docs/architecture.md) |
| 接口、错误和请求示例 | [api.md](docs/api.md) |
| 部署、迁移、备份、恢复 | [deployment.md](docs/deployment.md) |
| AI 平台调用、提示词、限制 | [ai-integration.md](docs/ai-integration.md) |
| AI 辅助开发过程记录 | [ai-development-log.md](docs/ai-development-log.md) |
| 演示讲解 | [demo.md](docs/demo.md) |
| 测试报告与复现 | [testing.md](docs/testing.md) |
| 公开验收记录与范围 | [verification/](docs/verification/) |
| 依赖与许可证 | [THIRD_PARTY.md](THIRD_PARTY.md) |

## 范围说明

已实现可运行的本机业务演示；当前没有真实支付退款、物流平台、短信邮件、企业 SSO 或用户注册。知识库面向小规模场景，最多 200 个有效片段，使用 Java 内存计算排序，不宣称使用 pgvector 或大规模向量数据库。尚未完成目标服务器、公网 HTTPS 或生产负载验收。迁移后仍需按实际业务要求验证，不能用本机测试替代。

## 开源与参与

本项目原创源码和文档采用 [MIT License](LICENSE)，第三方依赖和模型保留各自许可证，详见 [第三方说明](THIRD_PARTY.md)。欢迎通过 Issue 提交可复现问题，或参考 [贡献指南](CONTRIBUTING.md) 提交改进。
