# 公开验收记录

这里保留 2026-10-30 初版和 2026-10-31 Spring Boot 4 升级后的本机测试证据。演示用户、订单和商品均为虚构。当前提交是否通过自动构建，请查看 [GitHub Actions](https://github.com/daihuohuo2/java-ai-service-desk/actions/workflows/ci.yml)。

升级后的最终结果位于 `boot-upgrade-20261031/`：

- `junit-h2/`、`junit-postgres/`：同一组 49 项用例分别通过；XML 已移除环境属性和控制台输出。
- `http-final/summary.json`：19 项 HTTP 检查。
- `live-final/summary.json`：12 个模型场景及 1 个工单闭环；未知商品场景有一次安全降级。
- `live/` 和 `planner-diagnostic.json`：保留首次模型失败与排查记录，最终结果以 `live-final/` 为准。
- `dependencies-maven.txt`：当次升级解析出的 Maven 依赖树。

`ui/` 是应用界面的历史截图，不是当前公开在线服务。其他目录保留初版、中间中断及重试结果，解读时应结合 [测试报告](../testing.md) 的日期和边界。

历史报告中引用的原始 `.log`、`containers.json`、`start-script.txt`、`jar-sha256.json` 和 `vscode-zero-problems.png` 属于机器诊断材料，未提交到公开仓库；本地原文件保留。数据库、备份、模型权重、`.env`、旧版 ZIP 同样不在仓库内。

这些记录只证明对应版本当时完成了所列本机检查，不证明生产部署、通用模型准确率或所有硬件环境表现。
