# 第二课：实体、DTO、ORM 与类型转换

## 数据经过哪些类型

以商品修改为例：浏览器的 ProductDraft → JSON 请求 → ProductInput → ProductServiceImpl → ProductResponse → DeskConverter.productEntity → Product → ProductMapper.updateById → products 表。读取方向则是数据库行 → Product → converter.product → ProductResponse → JSON → TypeScript Product。

请求 DTO 用来接收用户输入，实体用来对应数据库，响应 DTO 用来决定给调用者看什么。这三个目的可能有相同字段，但不应因为相似就无条件共用。例如 User 有 passwordHash，UserResponse 没有；TicketResponse 没有 requestKey/requestHash。

## Product.java 里的注解

- `@TableName("products")` 指定数据库表；类名和表名可以不一样。
- `@TableId(type=INPUT)` 表示 ID 由应用提供。当前业务创建 UUID，不依赖数据库自增。
- priceCents 与 price_cents 通过下划线转驼峰规则对应。
- `@Getter/@Setter/@NoArgsConstructor` 由 Lombok 在编译时生成访问方法和无参构造器。源码短不等于没有这些方法；在 IDE 使用 Go to declaration 或 delombok 查看。
- 部分实体里的 `@TableField(exist=false)` 表示联表显示字段，不参与表的通用 CRUD。
- `@JsonIgnore` 是序列化保护；更主要的边界仍是选择安全的响应 DTO。

## MyBatis-Plus 用在了哪里

`ProductMapper extends BaseMapper<Product>` 提供 selectById、selectList、insert、updateById。ProductRepository 用 LambdaQueryWrapper 组装 active 条件和排序；`Product::isActive` 是方法引用，不是字符串 "active"。Mapper 是 MyBatis 生成的代理，因此没有手写 ProductMapperImpl。

`OrderMapper` 也有实体映射，但列表、分页、归属查询保留显式 JOIN SQL。复杂 SQL 不必强行拼装成 Wrapper。工单的 FOR UPDATE、索引任务的带 lease_token 条件更新仍在类型化 JDBC Repository 内，用来展示事务和并发控制。

本项目选择 MyBatis-Plus，没有再叠加 JPA。能解释一种 ORM 的映射、参数和事务，比为了清单安装两种更有学习价值。

## MapStruct 与 record

`DeskConverter` 在 app 模块，是 `@Mapper(componentModel="spring", unmappedTargetPolicy=ERROR)` 接口。真正的转换代码在编译后生成的 `backend/desk-app/target/generated-sources/annotations/cn/liuyang/desk/converter/DeskConverterImpl.java`。打开它，能看见普通的 getter → 构造器/setter 调用，并不是运行时反射魔法。

给 ProductResponse 新加一个无法映射的字段会导致编译失败；这是防止“加了字段但忘了转换”的机制。TicketResponse.events 显式忽略，随后通过单独的事件查询设置。

DTO 中 record 是 Java 的正规语言特性，适合不可变数据。它自动提供构造器、访问器、equals/hashCode/toString；`order.id()` 与实体的 `order.getId()` 风格不同。实体采用可变 Bean 是为了持久化和映射演示，不代表 record 错误。

## 动手练习

1. 找到 ProductResponse.priceCents 的 JsonProperty，解释为什么前端叫 price_cents。
2. 比较 ProductRepository 和 OrderMapper：哪些 SQL 由框架生成，哪些由你手写？
3. 阅读 FullstackIntegrationTest 中订单金额快照测试。修改商品价格后，旧订单 total_cents 为什么不变？
4. 给字段增加长度约束时，先补 DTO 测试，再检查数据库列长度、前端 maxLength 是否一致。

参考的是已锁定依赖版本的官方资料：[MyBatis-Plus 安装](https://baomidou.com/getting-started/)、[MapStruct 参考](https://mapstruct.org/documentation/stable/reference/html/)、[Lombok Maven](https://projectlombok.org/setup/maven)。Boot 4 使用对应的 mybatis-plus-spring-boot4-starter；不要同时保留旧 MyBatis Starter。
