# HTTP 接口说明

入口为 `http://127.0.0.1:19082/api`。JSON 默认 UTF-8。会话 Cookie 为 `DESK_SESSION`，HttpOnly、SameSite=Strict；正式 HTTPS 环境应配置 Secure。所有非 GET 请求（含登录/退出）需 `X-CSRF-TOKEN`。

健康检查：通过 Nginx 访问 `/health`；Java 容器内部为 `/actuator/health`。两个路径不要混用。

先 `GET /auth/csrf` 获取 `{token,headerName}` 并保留 Cookie，再 `POST /auth/login` 发送 `{username,password}`。登录成功会旋转会话并清除旧 CSRF，再次获取 token。浏览器封装位于 `frontend/src/api.ts`，可运行 HTTP 客户端参考 `scripts/http-acceptance.mjs`。

| 方法 / 路径 | 请求要点 | 权限/行为 |
|---|---|---|
| GET /meta | 无 | 环境和版本信息 |
| GET /auth/csrf | 无 | 取得会话 CSRF |
| POST /auth/login | username, password | 登录，密码错误限流 |
| GET /auth/me | 无 | 当前用户 |
| POST /auth/logout | 无 | 会话失效 |
| GET /products | 无 | 已上架商品 |
| GET /orders | 无 | 本人订单 |
| GET /orders/{id或订单号} | 无 | 本人订单详情，否则 404 |
| GET /tickets | 无 | 客户本人/客服全部，最多 200 |
| GET /tickets/{id} | 无 | 详情与 events |
| POST /tickets | orderId?,kind,subject,description | Idempotency-Key 必填 |
| POST /tickets/{id}/events | version,status?,message? | 状态机、角色、版本校验 |
| POST /proposals/{id}/confirm | {} | 仅草稿所属客户；重复确认返回同一工单 |
| GET /chat/threads | 无 | 本人最近 50 个会话 |
| POST /chat/threads | {} | 新建本人会话 |
| GET /chat/threads/{id}/messages | 无 | 本人历史消息和 detail 快照 |
| POST /chat/ask | threadId,question | JSON 完整结果；Idempotency-Key 必填 |
| POST /chat/stream | 同上 | SSE：phase、result、done 或 error |
| GET /knowledge | 无 | 管理员文档与索引状态 |
| POST /knowledge | title,productId?,content | 管理员新增 TXT/MD 内容 |
| POST /knowledge/upload | multipart: title,productId?,file | UTF-8 TXT/MD，文件最大 256 KB |
| PUT /knowledge/{id} | active | 启用/停用知识 |
| POST /knowledge/reindex | {} | 为缺少向量的有效片段建索引 |
| POST /knowledge/{id}/reindex | {} | 重新向量化指定有效文档 |
| GET /admin/overview | 无 | 客服/管理员统计和最近运行 |
| GET /admin/audit | 无 | 管理员最近 100 条审计 |
| GET /admin/products | 无 | 管理员全部商品 |
| POST /admin/products | name,category,priceCents,description,active | 管理员新增 |
| PUT /admin/products/{id} | 同上 | 管理员修改/下架 |

## 示例请求

```json
{"orderId":"order-1","kind":"REFUND","subject":"阅读灯退货申请","description":"商品未使用且包装完整，希望申请退货。"}
```

类型为 `REFUND/EXCHANGE/REPAIR/QUESTION`；前三种须关联已签收的本人订单。此校验只是受理申请条件，不代表最终审核通过。标题最多 120 字，描述最多 2000 字；咨询最多 1000 字。幂等键为 8–70 位字母、数字或连字符，推荐 UUID。

```json
{"version":0,"status":"IN_PROGRESS","message":"已接单，正在核对订单与问题。"}
```

更新成功会返回 `version + 1`，后续必须使用新版本。金额如 `19900` 表示 ¥199.00。时间为 ISO-8601 UTC，网页按浏览器本地时区展示。

## AI 返回对象

`runId, mode, promptVersion, intent, tool, answer, citations, latencyMs`；按意图附带 `orders` 或 `proposal`。`mode=OLLAMA` 表示本次模型计划有效；`RETRIEVAL_ONLY` 表示主动禁用模型；`MODEL_UNAVAILABLE` 表示模型失败并降级。不能只根据 HTTP 200 判定模型工作正常。

## 错误与重试

常规错误体为 `{"error":"说明"}`。400 参数不合法；401 未登录；403 角色/CSRF/Origin 不允许；404 无资源或不属于本人；409 版本/状态/幂等冲突；410 草稿过期；413 文件过大；429 登录或咨询限流；503 模型相关操作不可用。CORS 框架拒绝可能是纯文本 403。

网络超时后不要生成新幂等键立即重发写操作；先检查工单或会话，再使用原始键核对。SSE 已开始后，业务失败通过 `event:error` 返回，前端不可将其当作成功结果。
