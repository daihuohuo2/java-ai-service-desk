# 第五课：真实 Git 演进与动手练习

原仓库在本次开始时已有三个提交，基线是 4ddb270，并非零历史。但幂等、版本锁等之前的实现没有逐功能提交，不能通过伪造过去的 commit 解决。本次新增的每一步都来自实际修改与检查。

| 提交 | 阅读重点 |
|---|---|
| 354cfa1 | 记录基线和改造计划 |
| 57161f4 | 先分包、统一格式，业务行为保持，旧测试继续通过 |
| c979f5c | 实体/DTO、MyBatis-Plus、MapStruct、服务接口、Repository、纯规则和 Mockito 测试 |
| 5f42efa | 响应包装、前端适配、校验组、自定义约束及接口测试 |
| 31190eb | 用 Maven 模块表达 common/domain/app 的单向依赖，更新部署和打包路径 |
| 05e5b78 | 控制分支可读性、前端完整类型、商品编辑和聊天历史浏览器用例 |
| e0ba706 | 控制器显式声明响应类型，补齐 HTTP 层的类型边界 |
| 后续提交 | 通过 git log 查看学习材料和最终验证记录 |

```powershell
git log --oneline --reverse 4ddb270..HEAD
git show --stat 57161f4
git show c979f5c -- backend/src/main/java/cn/liuyang/desk/service/impl/OrderServiceImpl.java
git diff 4ddb270..HEAD -- backend
```

历史路径就是提交当时的路径。31190eb 之后源代码位于各 Maven 模块中。阅读记录不需要切换工作目录里的文件。需要在某个阶段练习时，可创建隔离 worktree；先确认父目录没有同名文件夹。

```powershell
git worktree add ../java-ai-service-desk-practice -b practice/order-filter HEAD
```

## 练习 A：先理解为什么需要幂等

运行 `java learning/IdempotencyVersionLab.java`。实验有四步：重复点击生成两张单 → 用请求键识别同一次提交 → 同键不同内容冲突 → 用版本阻止旧数据覆盖。它是单进程内存教学示例，不代替真实数据库锁和唯一索引。真实实现对应 TicketServiceImpl、TicketRepository 和 HTTP 并发验收脚本。

自己改动：去掉内容比对，让同一请求键带不同标题。先预测旧结果会被当作什么返回，再恢复断言。不要把这种错误版本合入可运行服务。

## 练习 B：新增一个查询条件

目标：给订单分页增加商品分类筛选。先写能区分同用户不同分类的测试，再修改 Controller 参数、Service 接口/实现、Mapper count/page 两条 SQL、前端搜索参数和页面。注意 count 和 page 必须使用相同筛选，否则总数与列表不一致。

验收：原筛选仍通过；customer2 看不到其他用户订单；刷新 URL 后筛选保留；非法分类或超长参数有明确响应。完成一个能力后提交一次，不把尚未运行的功能标为完成。

## 练习 C：从字段变化走完整栈

目标：给商品添加可选的售后联系电话。先写数据迁移 V3（不要修改已执行的 V1/V2），再修改实体、输入/响应 DTO、转换器、校验、表单、测试。让 MapStruct 编译错误提醒你遗漏的字段。检查历史订单字段是否应该跟随商品改变，并写下判断理由。

## 练习 D：认识测试边界

目标：给 TicketPolicyTest 添加“OPEN 不能直接 CLOSED”的纯规则测试，并用 MockMvc 写对应 API 测试。对比两种测试启动速度和能发现的问题。测试通过后记录真实命令及结果，再提交。

参考答案方向：B 的核心是所有查询条件一致且 owner 来自 Actor；C 的核心是 schema/Java/JSON/TS 四端一致；D 应收到 409，不能误判成参数校验错误 400。完整实现留给你练习，当前主分支维持已有业务。
