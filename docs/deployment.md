# 部署与迁移手册

## 2026-11-02 可选全栈组件

默认 PostgreSQL/H2 路径仍可运行。Redis、RabbitMQ、独立 MySQL 与 Prometheus 的组合命令见 [扩展说明](fullstack-upgrade.md)，测试环境见 [实验运行说明](learning/实验运行说明.md)。

- Redis 商品缓存与 RabbitMQ 索引队列：`compose.middleware.yaml`，部署模式不映射宿主服务端口。
- 独立 MySQL：`compose.mysql.yaml`，默认网页端口 19083，独立数据卷；不会迁移或覆盖 PostgreSQL 数据。下方备份/恢复脚本仅适用于 PostgreSQL。
- Prometheus：`compose.observability.yaml`，本机 19090；管理 HTTP 9090 仅在容器网络开放，Nginx 只代理健康检查。不要直接把管理端口暴露到公网。规则见 `deploy/alerts.yml`，尚未配置外发告警。
- Linux HTTPS 模板：`deploy/nginx.https.example.conf`，需替换域名和证书，设置 APP_ORIGIN、COOKIE_SECURE。模板不代表已完成真实公网 TLS 验收。
- 故障观察：`scripts/diagnose.ps1` 只读端口与 Java 诊断，不停止服务。数据库迁移前备份、先用新库验证，避免把测试指向业务数据库。


## 1. 环境

源码构建：JDK 21、Node.js 24、npm、Docker Engine/Desktop 与 Compose v2。容器运行 JRE 使用 Eclipse Temurin；源码无需付费 JDK。Maven Wrapper 与 npm lockfile固定构建依赖。首次构建/部署需联网下载依赖、镜像和模型。

本次机器为 Windows + RTX 4060 Laptop 8 GB；本机 GPU 模式已验收。CPU 模式和其他机器的耗时需要在目标机器测量，不能照搬本机结果。建议给 Docker 足够内存，模型服务和应用合计可预留 10 GB 以上；这是部署建议，不是已验证的最低配置。

## 2. Windows 快速启动

```powershell
git clone https://github.com/daihuohuo2/java-ai-service-desk.git
cd java-ai-service-desk
.\scripts\build.ps1
.\scripts\start.ps1 -ManagedModels -Gpu
```

从 GitHub 克隆后必须先 build；源码仓库不包含编译产物。只有自行生成、已含 `backend/target/service-desk-1.0.0.jar` 和 `frontend/dist` 的交付包才可省略 build。`init-env.ps1` 只在 `.env` 不存在时生成随机数据库密码，不覆盖现有配置。默认只绑定 `127.0.0.1:19082`，数据库和 Java 端口不对外发布。

`-ManagedModels` 创建本项目独立 `desk-models` 卷并下载两个模型，不依赖旧项目。无 NVIDIA GPU 则省略 `-Gpu`。本机验收另用 `java-ai-service-desk-ollama` 模型容器，只读复用已有权重，通过 `.env` 的 `OLLAMA_URL=http://host.docker.internal:28434` 接入；重新拉起当前环境用 `scripts/start.ps1`，不需要同时启动第二个模型服务。

容器视角的 `localhost` 指容器自身。外部宿主机模型地址应为 `host.docker.internal`；独立模型 Compose 覆盖文件使用 `http://ollama:11434`。

## 3. Linux 服务器迁移

克隆仓库或解压自行生成的交付包到独立目录；复制 `.env.example` 为 `.env`，将数据库占位符替换为 `openssl rand -hex 32` 生成的随机值。权限设为 `chmod 600 .env`。从 GitHub 克隆后执行 `sh scripts/build.sh` 编译。

```sh
docker compose -f compose.yaml -f compose.models.yaml up -d ollama
docker compose -f compose.yaml -f compose.models.yaml exec -T ollama ollama pull qwen3:4b-instruct-2507-q4_K_M
docker compose -f compose.yaml -f compose.models.yaml exec -T ollama ollama pull qwen3-embedding:0.6b
docker compose -f compose.yaml -f compose.models.yaml up -d --build --wait
```

有 NVIDIA Container Toolkit 时在上述 Compose 命令中都加入 `-f compose.gpu.yaml`。验收入口为本机回环地址，正式域名和 TLS 由目标服务器反向代理提供，不能直接把演示账户暴露到公网。

没有模型时，可将 `AI_ENABLED=false` 验收手动工单、订单、关键词检索；这不算完成真实 AI 验收。启动后管理员进入知识库，点击“更新向量索引”，首次为 12 个演示片段建索引。

## 4. 配置要点

| 变量 | 用途 |
|---|---|
| DATABASE_PASSWORD | PostgreSQL 口令，首次初始化后修改 env 不会自动改数据库已有密码 |
| WEB_PORT / BIND_HOST | 默认 19082 / 127.0.0.1 |
| OLLAMA_URL | 模型服务地址，不附带云密钥 |
| CHAT_MODEL / EMBED_MODEL | 对话和嵌入模型标签，变更嵌入模型后重新索引各有效文档 |
| MODEL_KEEP_ALIVE | 默认 5m，显存不足可设 0s，以更多加载耗时换取释放显存 |
| AI_ENABLED | 是否调用真实模型 |
| APP_ORIGIN | 允许 Origin，多个用逗号隔开，须与访问域名/端口一致 |
| APP_DEMO | 默认 true，创建虚构用户/订单/知识 |
| ADMIN_PASSWORD | 管理员首次创建时的密码，不用于更新已有密码 |
| COOKIE_SECURE / DEPLOYMENT_MODE | HTTPS 正式环境分别设 true / production |

`APP_DEMO=false` 不会删除已经存在的演示用户；正式环境必须使用新建的独立数据库并设置至少 14 字符且非默认值的管理员密码。当前项目没有正式客户注册入口，需要通过业务身份系统或实施脚本导入账户及订单。改成正式模式还需要实际业务规范、身份管理、TLS、监控和备份计划，不能仅修改一个开关就宣称投产。

## 5. 停止、重启与升级

```powershell
docker compose stop
docker compose start
docker compose ps
docker compose logs --tail 80 api
```

普通停止/重建不删除 `desk-data`。不要在保留数据时执行 `down -v`。升级前先备份，保留旧 JAR/镜像和配置，在隔离环境跑迁移及验收，再切换流量。数据库迁移向前执行，回滚应用前必须确认数据库仍兼容旧版本。

## 6. 备份与恢复

```powershell
.\scripts\backup.ps1
.\scripts\verify-restore.ps1 -Backup .runtime\backups\desk-YYYYMMDD-HHMMSS.dump
```

备份使用 `pg_dump -Fc`，通过 docker cp 保存二进制，避免 PowerShell 文本重定向破坏数据；附带 SHA256。备份包含用户密码哈希及业务数据，应仅保存在受控目录，交付 ZIP 不包含本机备份。

恢复验证脚本创建随机命名、无网络、无宿主端口的临时 PostgreSQL 容器，执行 `pg_restore --exit-on-error`，读取恢复后的数据计数，最后只移除该测试容器。现有数据库不会被覆盖。本机验收记录见 `verification/restore.txt`。

正式恢复应恢复到新的数据库/数据卷，核对计数、约束、最近工单和 Flyway 版本，再把应用切换到新库。模型权重无需随数据库备份，按固定标签与 digest 重新下载；自定义知识内容、片段向量和历史引用都在数据库内。

## 7. 故障定位

- 403：核对 Origin、登录状态和 CSRF，登录后须刷新 token；不要关闭 CSRF 解决问题。
- 502/健康检查失败：检查 `docker compose ps`、数据库健康与 api 日志，等待 Flyway 完成。
- AI 显示降级：检查 Ollama `/api/tags`、模型名、容器网络和显存；看 `chat_runs.model_mode`，不是只看 HTTP 200。
- Windows 端口无法绑定：用 `netsh interface ipv4 show excludedportrange protocol=tcp` 检查保留端口，调整 WEB_PORT/APP_ORIGIN。
- 模型冷启动慢：保持 5m 驻留、允许两个模型同时驻留；CPU/显存不足时会更慢，优先换小模型或专用模型服务。
- 本次所有测试基于虚构数据。指定服务器、公开域名和真实外部业务接口尚未提供，现场部署仍待完成。
