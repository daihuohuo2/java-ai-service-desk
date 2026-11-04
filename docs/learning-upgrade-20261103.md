# Java 全栈学习版交付说明

工作目录：`D:\Project\java-ai-service-desk`；本地分支：`learning/typed-layers-20261103`。学习入口是 [docs/learning/README.md](learning/README.md)。本次保留既有业务和数据库结构，以源码教学、类型约束、工程边界及可验证改动为重点。

## 交付内容

- 后端三个 Maven 模块：desk-common、desk-domain、desk-app；可执行 JAR 为 `backend/desk-app/target/service-desk-1.0.0.jar`。
- 分层包、9 个业务接口及实现、类型化实体/DTO；商品采用 MyBatis-Plus CRUD，订单保留联表 SQL，工单及任务的锁与 CAS 在 Repository 内。
- Lombok、MapStruct、Create/Update 校验组、自定义 EnumValue、统一 JSON 响应；前端类型和请求封装同步更新。
- 订单逐句讲解、变量轨迹、全部源文件职责表、读取顺序、五课学习材料、独立 Java 实验、真实 Git 提交阅读路线。
- 新增本机学习启动器、固定版本 Java 格式检查、更新构建/容器/打包/CI 路径。

## 当前验收结果

详见 [机器可读摘要](verification/learning-20261103/summary.json)。测试全部使用虚构数据。

| 验证 | 结果与边界 |
|---|---|
| H2 聚合 verify | 113 项发现，110 通过，3 项中间件测试按条件跳过 |
| PostgreSQL + Redis + RabbitMQ | 113/113 通过；独立测试容器，模型为 Mock |
| MySQL | 110 通过，3 项中间件测试跳过；相同业务套件的跨数据库重跑，不是额外 110 种功能 |
| 纯单测 profile | 38/38；Mockito 或纯 Java/校验，不启动 Spring 应用 |
| 前端 | 13 单测通过，TypeScript strict 编译及 Vite 构建通过 |
| Chromium | 18/18；桌面、手机尺寸，包含新增/编辑商品、SSE 订单结果及历史重载 |
| HTTP 会话、权限和并发 | 19/19 |
| 完整业务与后台索引 | 14/14，真实模型嵌入任务成功 |
| 本机真实模型 | 12 个问答场景 + 1 个工单闭环通过；11 次 OLLAMA，1 次 MODEL_UNAVAILABLE 安全降级 |
| Java 学习实验 | 基础、并发、幂等/版本实验均通过 |
| 容器与材料 | 新模块路径的 Docker 构建通过；学习文档源文件链接检查通过 |

未知商品场景的模型计划未能通过解析校验，触发 MODEL_UNAVAILABLE，最终返回资料不足，没有编造产品信息。该用例的安全降级断言通过，不表示本次 12 次问答全部由模型成功完成，也不构成通用准确率评测。

上述均为本机验证。本次没有推送 GitHub、执行远程 Actions 或部署到任务方指定服务器。原有旧日期报告继续作为历史证据；README 的远程徽章不能代替本次分支验收。

## 使用方法

```powershell
cd D:\Project\java-ai-service-desk
# 完整构建
.\scripts\build.ps1
# 先学习普通 Java/Web 功能，不要求模型和 Docker
node scripts/dev-learning.mjs
# 若本机 Ollama 已有对应模型，可启用 AI
node scripts/dev-learning.mjs --ai
```

学习网页 http://127.0.0.1:21985，API 为 21984。Ctrl+C 停止该学习实例；持久数据位于 `.runtime/learning`。演示账号沿用 README。Docker 正式形态启动仍用 `scripts/start.ps1`；它会使用原 `.env`，学习启动器与之隔离。

## 交付包

`dist/java-ai-service-desk-learning-20261103.zip` 包含三个模块源码、前端源码及构建产物、可执行 JAR、文档、测试、部署脚本和逐文件 SHA-256 清单。旁边提供 ZIP 的 SHA-256 和验证 JSON。包中排除 `.env`、本地数据库、模型权重、node_modules 和本地工具缓存；首次重新编译/启动 Vite 仍需下载相应依赖。

Git 历史保留在当前工作目录的 `.git`；ZIP 不直接包含 `.git`，另内置 `dist/java-ai-service-desk-learning-history-20261103.bundle`，可在其他机器恢复本次分支。要阅读真实提交过程，在当前项目目录使用 `git log --oneline --reverse 4ddb270..HEAD`。没有重写、伪造过去的功能提交。

解压后需要恢复 Git 历史时，克隆 bundle 到一个新的目录：

```powershell
git clone -b learning/typed-layers-20261103 dist/java-ai-service-desk-learning-history-20261103.bundle ../java-ai-service-desk-with-history
```

原始简历和旧版交付包未改动。本次新增加的是学习内容；未来填写简历时应区分本人理解与练习完成程度、功能实现和实际生产部署。
