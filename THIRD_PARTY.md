# 第三方依赖与素材

本次全栈扩展新增 MyBatis / MyBatis Spring Boot Starter、Spring Data Redis、Spring AMQP、MySQL Connector/J、Flyway MySQL、Micrometer Prometheus、React Router、Playwright 和 Prettier；可选容器包括 MySQL 8.4、Redis 8.2、RabbitMQ 4.1、Prometheus 3.5。它们各自的 LICENSE/NOTICE 和镜像条款继续适用，项目 MIT 不重新许可这些依赖。容器、模型及 npm/Maven 依赖不会提交到 GitHub 源码仓库；交付 JAR 包含运行依赖，分发时应保留相应声明。实际版本以 pom、锁文件和 Compose 为准。


本项目业务代码和界面在本次任务中创建，演示商品、政策、订单和姓名均为虚构，没有复制真实客户资料。图标使用 lucide-react，无外链字体、商业图片素材或闭源付费 SDK。

主要直接依赖：Spring Boot/Security/AI、Apache Maven、Flyway、JUnit（各自开源许可证）；PostgreSQL（PostgreSQL License）；H2（MPL 2.0 / EPL 1.0 双许可）；React、Vite、TypeScript、Ollama（各项目许可证）；Lucide（ISC）。实际发布再分发应保留所用依赖的许可证及通知文件，不能把本表当成完整的法律审核。

Qwen 官方模型卡标示 Apache-2.0：[Qwen3-4B-Instruct-2507](https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507)、[Qwen3-Embedding-0.6B](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B)，查阅日期 2026-10-30。模型通过 Ollama 官方注册表拉取，交付包不打包模型权重。

容器运行 Java 使用 Eclipse Temurin 21；构建可使用同版本开源 JDK。Docker Desktop 属于本机工具，使用方按其自身适用条款使用；Linux 服务器可以使用 Docker Engine。系统本身不依赖 Docker Desktop 专属 API。

完整版本由 `backend/pom.xml`、Maven 依赖树、`frontend/package-lock.json` 与 `compose*.yaml` 记录。历史依赖树在 `docs/verification/dependencies-*`，Boot 4 升级后的 Maven 依赖树在 `docs/verification/boot-upgrade-20261031/dependencies-maven.txt`；当前解析结果以锁文件和实际构建为准。

经项目所有者选择，原创业务源码、界面和文档以 [MIT License](LICENSE) 发布，版权署名为 GitHub 账号 `daihuohuo2`。MIT 声明不替代第三方依赖、Maven Wrapper、图标或模型各自的许可。GitHub 源码仓库不包含模型权重、依赖二进制包、本机数据库或个人配置，也不代表向任何活动主办方提交或转让权利。
