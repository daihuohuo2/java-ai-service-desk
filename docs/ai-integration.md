# AI 平台与提示词调用说明

## 运行链路

使用 Spring AI 2.0.2 的 `ChatModel` 与 `EmbeddingModel` 对接本地 Ollama HTTP API，与 Spring Boot 4.1.2 配套。模型配置为 `qwen3:4b-instruct-2507-q4_K_M` 与 `qwen3-embedding:0.6b`，分别位于 `spring.ai.ollama.chat.model` 和 `spring.ai.ollama.embedding.model`。没有云账号、API Key 或按 token 计费的必需依赖；推理消耗本机算力。

请求先检索知识：UTF-8 TXT/MD 按空行分段，长段每 600 字切分，单文档最多 60 段、30000 字，当前最多 200 个有效片段。向量与模型名保存在关系数据库字段中；查询时对有效片段计算余弦相似度，结合中文二元字符命中率排序，识别商品时过滤其他商品资料，取前 5 个候选。

对话模型接收系统提示词、当前问题、最近最多 4 条用户问题及候选资料，返回受限 JSON 计划：

```json
{"intent":"KNOWLEDGE","orderNumber":"","kind":"QUESTION","sourceIds":["S1"]}
```

后端只接受四种 intent：KNOWLEDGE / ORDER / AFTER_SALES / HUMAN；kind 只允许 REFUND / EXCHANGE / REPAIR / QUESTION；订单号格式和 sourceIds 数量/来源再次校验。模型不执行任意代码、不访问数据库、不直接写工单。

这是“模型生成受限计划 → Java 白名单分派”的实现，不是声称模型原生 Function Calling，也没有使用 pgvector、微调或多 Agent 编排。

## 可信回答与操作确认

KNOWLEDGE：Java 为本次请求的候选片段分配 S1、S2 等短编号，模型选择能回答问题的编号，Java 通过本次请求的映射还原数据库 ID，再引用对应原文。未知短编号将使整组引用失效，不能使用其他请求的编号或直接传入数据库 UUID 绕过映射。没有资料时说明资料不足，不自动补全参数。这个版本优先可核验性，回答偏引用式，不做自由改写。短编号避免小模型抄写长 UUID 时出现漏字或错字；接口最终返回的引用 ID 仍为真实数据库 ID。

ORDER：Java 用当前登录用户查询本人订单；跨账号订单统一返回未找到。即使模型被误导产生订单号也不能突破所有权检查。

AFTER_SALES / HUMAN：Java 整理 15 分钟有效的草稿，前端显示确认按钮。客户点击后再次核验用户、订单、期限和幂等性才创建工单。AI 和此系统都没有真实扣款或退款执行能力。

知识和用户消息都视为不可信数据；不将文档里的“忽略规则”等文本提升成系统指令。仍不能把有限注入测试解释为对所有提示注入绝对免疫，安全边界依赖 Java 权限与业务校验。

## 参数、超时、降级与观测

temperature=0，format=json，num_ctx=4096，num_predict=256；模型驻留默认 5m。单次模型调用 90 秒、HTTP 读取 95 秒；最多两个模型任务并发，每个客户一条咨询。检索和对话均可能触发模型加载，所以冷启动明显慢于热请求。

模型输出 JSON 非法、超时或容量不足：标记 `MODEL_UNAVAILABLE`，仅做只读关键词检索或精确订单号查询，不推断写操作。主动 `AI_ENABLED=false` 使用 `RETRIEVAL_ONLY`。`OLLAMA` 表示本次计划来自模型且通过结构校验，不代表回答普适准确。

每次运行保存 `runId / promptVersion / question / state / model_mode / latency_ms / result_json`；消息保存引用原文快照。管理员/客服概览只显示运行状态与耗时，详情仍受会话所有权约束。

## 提示词与复现

完整系统提示词唯一来源为 `backend/desk-app/src/main/java/cn/liuyang/desk/ai/AiGateway.java` 的 `SYSTEM` 常量，版本 `planner-v2-short-refs`。真实测试脚本 `scripts/live-acceptance.mjs` 保留问题、判定、结果、耗时；升级后的证据位于 `docs/verification/boot-upgrade-20261031`，`docs/verification/live-postgres` 是 2026-10-30 旧版本的历史记录。

```powershell
$env:BASE_URL='http://127.0.0.1:19082'
node scripts/live-acceptance.mjs
```

该脚本使用演示账户，会创建会话、草稿和一张工单并走完整闭环；只用于隔离演示数据。不要与 HTTP 脚本或同账号浏览器咨询同时运行，否则同用户并发保护会正确拒绝第二条咨询。

模型卡与接入参考：[Qwen3 4B Instruct](https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507)、[Qwen3 Embedding 0.6B](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B)、[Spring AI Ollama](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)。本项目以锁定依赖实际编译运行结果为准，在线文档可能已指向更高版本。
