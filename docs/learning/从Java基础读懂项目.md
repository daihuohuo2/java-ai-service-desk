# 从 Java 基础读懂项目

当前代码已经从早期单包 Map 版本改为分层、强类型、多模块版本。请按 [第一课：订单请求与变量轨迹](01-订单请求与变量轨迹.md) 阅读，它包含字段、构造器、全部订单方法、启动与请求顺序、具体数据和异常分支。

先运行 `java learning/FullstackBasics.java`，再看 [文件阅读顺序](文件阅读顺序.md)。原来讨论的 `this.s=s` 是保存 Store 依赖；现在 OrderServiceImpl 的 `this.orders=orders` 保存 OrderMapper，依赖注入的 Java 原理相同。

旧实现可通过 `git show 4ddb270:backend/src/main/java/cn/liuyang/desk/OrderService.java` 阅读；这只是历史对照，当前运行入口在 desk-app 模块。不要把旧文档中的 Map 字段访问照搬到新实体上。
