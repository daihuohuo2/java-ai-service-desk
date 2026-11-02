# 测试与验收报告

## 开源仓库自动测试

[GitHub Actions](https://github.com/daihuohuo2/java-ai-service-desk/actions/workflows/ci.yml) 在推送和 Pull Request 时运行 Java 21 后端测试（H2 与独立 PostgreSQL 17）、9 项前端测试及生产构建。CI 使用 Mock 模型，不替代真实 Ollama、GPU、浏览器或服务器验收。具体状态以对应提交的 Actions 结果为准。

下文是带日期的历史本机验收。公开仓库保留摘要和虚构业务场景；机器日志、绝对路径清单和桌面截图仅在本机留存，详见 [证据说明](verification/README.md)。

## 2026-10-31 Spring Boot 4 升级验收

当前版本为 Spring Boot 4.1.2 + Spring AI 2.0.2，Java 21。H2 和 PostgreSQL 分别通过同一组 49 项后端测试（原 44 项加 5 项模型引用映射回归测试）；9 项前端测试、生产构建、19 项 HTTP 检查、12 个模型场景与工单闭环均通过。未知商品场景使用一次安全降级，不能把 13 项行为断言通过描述为模型准确率 100%。

当前原始证据位于 `verification/boot-upgrade-20261031/`。后端以 `backend-verify-final.log`、`backend-postgres-final.log` 为准，模型与接口以 `live-final/summary.json`、`http-final/summary.json` 为准。首轮失败记录和只读诊断已保留，最终代码已修复长资料 ID 抄写错误问题。详见 [升级验收报告](upgrades/20261031-spring-boot-4.md)。

以下保留 2026-10-30 的历史验收，版本和测试数量不代表当前代码。

日期：2026-10-30；范围：本机 Windows、Docker PostgreSQL、Ollama GPU、虚构业务数据。无外部服务器或支付退款操作。

| 验证 | 实际结果 | 证据 |
|---|---|---|
| JUnit / MockMvc / 业务测试，H2 | 44 通过，0 失败 | verification/junit-h2 |
| 同一组测试，PostgreSQL 17 | 44 通过，0 失败 | verification/junit-postgres |
| 前端请求与 SSE 解析 | 9 通过 | verification/frontend-tests.txt |
| TypeScript 与生产构建 | 通过 | verification/frontend-build.txt |
| 真实 HTTP / PostgreSQL | 19 通过 | verification/http-postgres/summary.json |
| 首轮冷启动模型场景与闭环 | 12 场景 + 1 闭环通过，12 场景均为 OLLAMA | verification/live-cold-start |
| 最终驻留设置复测 | 12 场景 + 1 闭环通过，其中 1 场景使用降级模式 | verification/live-postgres |
| PostgreSQL 备份及隔离恢复 | 恢复成功，用户 4、工单 6、消息 52、片段 12 | verification/restore.txt |
| 浏览器操作 | 登录、查单、手动建单、管理员知识/商品、移动菜单通过 | verification/ui |
| 交付 ZIP 解压部署 | 新 Compose 项目、新 PostgreSQL 卷、19085 端口，19 项 HTTP 再次通过 | verification/package-http |
| 应用重启与持久化 | 工单/消息/片段计数保持一致，健康 UP | verification/restart.json |

后端两组使用相同 44 个测试，不把它们说成 88 个不同用例；单元/集成测试中的 AI 为 Mock，真实模型效果单列。

## 真实模型效果与局限

用例包括阅读灯供电/App/保修、退货政策、未承诺的保温时长、未知商品、本人/他人订单、退款草稿、只问政策不操作、提示注入和人工咨询。

首轮每次卸载模型再加载，12 次均通过模型计划，端到端耗时约 29–70 秒。改为两个模型驻留 5 分钟后，最终一组多数热请求约 1–2 秒；未知商品请求出现 `MODEL_UNAVAILABLE`，系统安全拒答且未生成订单或草稿，故通过安全行为断言。该组另有约 8.5 秒订单请求。不能声称所有请求都在 2 秒内，也不能把 13 项断言通过说成模型准确率 100%。逐条耗时和运行模式保存在 JSON。

一次验收期间重建 Nginx，导致进行中的 HTTP 连接断开，记录保留在 `interrupted-by-redeploy`；原后台任务仍在执行时立即重试被 429 拒绝，记录在 `rejected-while-running`。待任务落库后串行重跑通过，验证了部署期间连接中断和同用户并发保护的实际行为。两份中间目录中的历史文件可能来自前一轮，判断每轮结果以其 summary 时间和失败项为准。

当前证据是有限、固定问题的本机验收，不是大规模效果评测、并发容量或生产安全认证。没有测试所有浏览器、真实移动设备、CPU 推理、跨机器重建、目标指定服务器与公网 HTTPS。

浏览器额外观察：省略品牌的“阅读灯需要充电吗”出现保守拒答；完整商品名问法可正常返回原文引用，因此首页示例使用完整商品名。这个版本对简称和上下文的理解仍需要扩展评测，未把拒答包装成正确命中。交付包重建验证为同机隔离应用和数据库，复用同一模型服务；不等同于另一台机器的全离线安装验证。

## 复现

```powershell
.\backend\mvnw.cmd -f backend/pom.xml test
npm.cmd test --prefix frontend
npm.cmd run build --prefix frontend
$env:BASE_URL='http://127.0.0.1:19082'
node scripts/live-acceptance.mjs
node scripts/http-acceptance.mjs
```

两个 HTTP 脚本串行执行，不要同时以 customer 在浏览器提问。会话/工单测试会往虚构演示数据库写测试记录；不可直接在生产数据库运行。

PostgreSQL 测试可另建独立测试库，通过 `TEST_DATABASE_URL/TEST_DATABASE_USER/TEST_DATABASE_PASSWORD` 注入，不使用演示库；再执行 Maven test。后端测试默认事务回滚，但启动时会初始化测试库，不应指向真实数据库。

前端测试重点：CSRF、会话凭证、401/502/204、FormData Content-Type、UTF-8 字节被拆分和 CRLF 的 SSE、业务错误事件、结果返回前断线。真实 HTTP 重点：会话旋转/退出、CORS、越权、并发重复建单、版本冲突、SSE 阶段和最终结果。
