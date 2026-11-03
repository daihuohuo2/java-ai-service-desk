# Java 全栈学习版：从一次请求读懂项目

当前版本从真实可运行的客服系统逐步重构，分支为 `learning/typed-layers-20261103`。先把订单查询读透，再逐步进入工单、数据库、前端和 AI。不要一开始同时学习所有框架。

## 先做这三件事

1. 用 IDE 打开项目根目录，把 `backend/pom.xml` 导入为 Maven 项目，选择 JDK 21；启用 annotation processing，以便生成 Lombok 和 MapStruct 代码。
2. 在根目录运行 `java learning/FullstackBasics.java`，再运行 `java learning/IdempotencyVersionLab.java`。这两个练习不需要数据库和 Spring。
3. 阅读 [第一课：订单请求与变量轨迹](01-订单请求与变量轨迹.md)，同时打开 `OrderService` 接口和 `OrderServiceImpl` 实现。现在执行代码在 **Impl** 中。

## 学习目录

| 顺序 | 内容 | 学完要能完成的动作 |
|---|---|---|
| 1 | [订单请求与变量轨迹](01-订单请求与变量轨迹.md) | 解释参数来自哪里、谁调用谁、结果如何回到网页 |
| 2 | [实体、DTO、ORM 与转换](02-实体DTO与ORM.md) | 画出数据库行 → Java 对象 → JSON 的对应关系 |
| 3 | [校验与统一响应](03-校验与统一响应.md) | 在浏览器 Network 中区分 400、401、403、409 |
| 4 | [测试、事务与工程边界](04-测试与工程边界.md) | 不启动 Spring，独立测试一个业务类 |
| 5 | [Git 演进和动手练习](05-Git演进与动手练习.md) | 从真实提交看变化，先写失败断言再修改实现 |
| 随时查阅 | [文件阅读顺序](文件阅读顺序.md)、[项目结构与文件职责](项目结构与文件职责.md) | 找到实现，不在目录里迷路 |
| 实际运行 | [实验运行说明](实验运行说明.md)、[全栈学习路线](全栈学习路线.md) | 运行单测、数据库测试、浏览器和学习服务器 |

## 对照你提出的改造点

| 要求 | 当前实现 |
|---|---|
| 分层包结构 | app 中的 controller / service.impl / mapper / repository / converter / security / config；domain 中的 service / entity / dto / domain；common 中的异常、响应、校验 |
| 消除业务 Map | Order、Product、User、Ticket、Proposal、聊天及知识实体；类型明确的响应、分页和检索结果。保留的 Map 用于状态转移表、引用编号字典、字段错误，含义固定 |
| 可读性 | 统一 Java 格式、描述性字段和方法名、控制器委托服务、SQL 放在 Repository/Mapper、纯状态规则独立 |
| Git 学习过程 | 在原有提交之上记录本次真实阶段，不重写历史；幂等与版本号的引入过程另有可运行实验 |
| ORM | MyBatis-Plus 的 ProductMapper 实际承担 CRUD，OrderMapper 保留复杂查询；其他聚合展示类型化 JDBC 与行锁/CAS |
| 接口与实现 | 9 个业务服务接口及对应 Impl，控制器只依赖接口 |
| Lombok / MapStruct | 实体访问器和构造器、依赖注入示例、编译期实体与响应转换；DTO 的 record 继续保留 |
| 分组、自定义校验 | ProductInput 的 Create/Update、EnumValue 与 TicketKind；字段错误回传给调用方 |
| 统一响应 | API JSON `{code,message,data}`，真实 HTTP 错误状态保留；CSV、SSE、204、Actuator 单独遵循各自协议 |
| Maven 多模块 | desk-app → desk-domain → desk-common；一个可部署应用，不是三个微服务 |
| 纯单测 | Mockito 的订单服务测试、模型与缓存替身测试、无 Spring 的状态机/校验测试；另保留真实数据库集成测试 |

本版仍是有明确规模边界的学习系统：知识库有效片段上限 200，客户限流有进程内状态，向量检索在内存中计算。订单展示现有数据，工单退款只记录申请，不接真实支付。这些是可继续扩展的练习，不应写成生产规模能力。
