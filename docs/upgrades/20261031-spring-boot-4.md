# Spring Boot / Spring AI 升级与验收

日期：2026-10-31。范围：本机源码、VS Code、Docker 演示部署与虚构业务数据。

## 版本与依据

| 组件 | 原版本 | 当前版本 |
|---|---|---|
| Spring Boot | 3.5.16 | 4.1.2 |
| Spring AI | 1.1.8 | 2.0.2 |
| Java | 21 | 21 |
| 模型计划协议 | planner-v1 | planner-v2-short-refs |

Spring AI 官方兼容说明确认 2.0.x 支持 Spring Boot 4.0.x / 4.1.x。使用正式发行版本，已从 Maven Central 实际解析和构建。VS Code 原 BOOT_VERSION_VALIDATION_CODE 警告来自 Boot 3.5 系列支持周期；此次更新依赖并重新导入项目，没有关闭或降低诊断级别。

- [Spring AI 兼容说明](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Spring Boot 受支持分支](https://github.com/spring-projects/spring-boot/wiki)
- [Spring Boot 4 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring AI 迁移说明](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)

## 实际改动

1. `backend/pom.xml` 更新 Boot parent 与 AI BOM，并整理为多行格式；Web MVC、Flyway、Web MVC 测试、安全测试使用 Boot 4 的模块 starter。
2. `Store` 和 `AiGateway` 迁移至 Jackson 3 的 `tools.jackson` 包；使用 `asString` / `isString` 读取模型计划。
3. `DeskApplication`、`HttpClients` 与 `DeskIntegrationTest` 采用 Boot 4 重组后的自动配置、HTTP 自定义器和 MockMvc 注解包名。
4. `application.yml` 的聊天、嵌入配置取消旧 `.options` 层，`AiGateway` 读取新的 embedding model 属性。
5. 真实模型首轮有两条阅读灯问答保守拒答。只读复核发现模型在复制长 UUID 时发生错字；将输入中的资料引用换成本次请求内的 S1/S2 短编号，Java 映射回真实 ID。未知编号使整组引用失效，仍保留候选集合校验。新增 5 项回归测试验证映射、隔离、绕过拒绝和非法 JSON 降级。未修改验收问题或放宽断言。
6. 更新 README、AI 说明和源码学习文件索引；交付脚本支持指定新 ZIP 名，保留原旧版 ZIP。

## 最终验证

证据基目录：`../verification/boot-upgrade-20261031/`。本表记录当时的本机验收；公开仓库保留结果摘要、脱敏 JUnit 与虚构业务场景，原始 `.log`、本机绝对路径清单和编辑器桌面截图仅在本机留存，详见 [公开证据说明](../verification/README.md)。

| 检查 | 结果 | 证据 |
|---|---|---|
| H2 后端完整测试、可执行 JAR 构建 | 49 通过，0 失败/错误/跳过 | backend-verify-final.log、junit-h2/ |
| 独立 PostgreSQL 17.6 测试库 | 同一组 49 通过 | backend-postgres-final.log、junit-postgres/ |
| 前端请求/SSE 单元测试 | 9 通过 | frontend-tests.log |
| TypeScript + Vite 生产构建 | 通过 | frontend-build.log |
| 真实 HTTP、CSRF、登录会话、角色权限、订单隔离、幂等、版本冲突和 SSE | 19 通过 | http-final/summary.json |
| 真实 Ollama 场景 + 售后工单完整闭环 | 12 场景 + 1 闭环通过；11 场景模式为 OLLAMA，未知商品 1 场景为 MODEL_UNAVAILABLE 并安全拒答 | live-final/summary.json 及各场景 JSON |
| 本机应用健康、前端入口 | UP / HTTP 200 | startup.json |
| 已部署 JAR 与本地最终 JAR 一致 | SHA-256 一致 | jar-sha256.json、container-jar-sha256.txt |
| VS Code Maven 导入与版本诊断 | Java Ready，0 错误，0 警告 | vscode-zero-problems.png |

原 44 项测试加 5 项回归测试，共 49 个不同用例；在两种数据库执行，不计为 98 个不同用例。JUnit 的 AI 为 Mock，真实模型效果单列。保留首轮模型失败的 `live/`、诊断的 `planner-diagnostic.json`，最终结果以 `live-final/` 为准。

测试日志中的 Mockito 动态 agent 提示、Flyway 对较新 H2 版本的验证范围提示仍如实保留；H2 49 项用例通过，实际容器使用 PostgreSQL 17。VS Code 零警告指编辑器诊断，不代表所有工具日志均无提示。

## 本机运行与数据

- 页面：<http://127.0.0.1:19082/>；健康：<http://127.0.0.1:19082/health>。
- 原 Compose `api` 已重建为升级版本，`web` 已重启以更新后端解析；数据库卷和模型保留。
- 第一次升级前后，用户 4、工单 6、消息 60、知识片段 12，数量一致；数据库迁移记录仍为 V1，无业务表迁移。之后 HTTP 与模型验收会追加演示工单和消息，属于测试记录，不是原数据丢失。
- 隔离 PostgreSQL 测试容器 `java-ai-service-desk-upgrade-tests-20261031` 已停止。

## 备份与回退

本机备份目录：项目根目录下 `.runtime/boot-upgrade-backup-20261031-155324/`，被 VS Code 导入、Git 和源码交付排除；它是原开发环境的历史备份，不是克隆后会自带的目录。

保留了升级前源码、配置、文档、原构建 JAR、原容器实际运行的 `running-app-before.jar`，以及 PostgreSQL 自定义格式备份 `desk-pre-boot4.dump`。数据库备份已用 `pg_restore --list` 读取检查。

如果需要回退应用，先停止测试和新请求，将备份中的源码/配置恢复到对应位置，将 `running-app-before.jar` 复制为 `backend/target/service-desk-1.0.0.jar`，再在项目根目录运行 `docker compose up -d --build --no-deps --wait api` 和 `docker compose restart web`。先只回退应用，不恢复数据库；本次没有业务表迁移，直接恢复旧数据库会丢失升级后新增的演示记录。如确需恢复数据，应另存当前数据库并在隔离库验证备份。

## 交付与边界

新版包为 `dist/java-ai-service-desk-1.0.0-boot4-20261031-delivery.zip`，包含当前源码、JAR、前端、配置、测试与文档；打包时逐文件验证 SHA-256 和 ZIP 完整性，排除 `.env`、本机数据库、模型权重和运行目录。旧版 ZIP 保留为历史归档。

本次已验证本机 Docker 启动和业务调用；新版 ZIP 的校验不等于在另一台机器解压部署验收。用户未提供对方服务器，因此未做指定服务器、公网 HTTPS、真实退款或生产负载验证。有限模型断言不代表通用准确率 100%。
