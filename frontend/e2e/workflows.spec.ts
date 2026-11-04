import { test, expect, Page } from "@playwright/test";
import fs from "node:fs/promises";

async function login(page: Page, role = "客户", route = "/orders") {
  await page.goto(route);
  await page.getByRole("button", { name: role, exact: true }).click();
  await page.getByRole("button", { name: "登录工作台" }).click();
  await expect(page.getByRole("button", { name: "退出登录" })).toBeAttached();
}

test("orders: paging, filtering, URL reload and owner isolation", async ({
  page,
}, info) => {
  await login(page);
  await expect(page.locator(".order-card")).toHaveCount(2);
  await page.getByLabel("每页条数").selectOption("1");
  await expect(page.locator(".order-card")).toHaveCount(1);
  await expect(page.locator(".order-card")).toContainText("ZS20261030001");
  await page.getByRole("button", { name: "下一页" }).click();
  await expect(page.locator(".order-card")).toContainText("ZS20261030002");
  await page.getByLabel("订单状态").selectOption("DELIVERED");
  await expect(page.locator(".order-card")).toContainText("ZS20261030001");
  await page.reload();
  await expect(page.getByLabel("订单状态")).toHaveValue("DELIVERED");
  await page.getByLabel("订单号关键词").fill("ZS20261030003");
  await page.getByRole("button", { name: "查询订单" }).click();
  await expect(page.getByText("没有符合条件的订单")).toBeVisible();
  await page.getByRole("button", { name: "重置", exact: true }).click();
  await expect(page.locator(".order-card")).toHaveCount(2);
  await page.screenshot({
    path: info.outputPath("orders.png"),
    fullPage: true,
  });
});

test("customer route guard and backend authorization both apply", async ({
  page,
}) => {
  await login(page);
  await page.goto("/knowledge");
  await expect(page).toHaveURL(/\/chat$/);
  expect((await page.request.get("/api/knowledge/jobs")).status()).toBe(403);
  expect((await page.request.get("/api/orders/order-3")).status()).toBe(404);
});

test("ticket form validates and creates a real question", async ({ page }) => {
  await login(page, "客户", "/tickets");
  await page.getByRole("button", { name: "创建工单", exact: true }).click();
  await page.getByLabel("工单标题").fill("   ");
  await page.getByLabel("问题描述").fill("希望获取商品使用说明");
  await page.getByRole("button", { name: "确认提交工单" }).click();
  await expect(page.getByRole("alert")).toContainText("空格");
  const title = "学习验收 " + Date.now();
  await page.getByLabel("工单标题").fill(title);
  await page.getByLabel("服务类型").selectOption("REFUND");
  await page.getByRole("button", { name: "确认提交工单" }).click();
  await expect(page.getByRole("alert")).toContainText("关联订单");
  await page.getByLabel("服务类型").selectOption("QUESTION");
  await page.getByRole("button", { name: "确认提交工单" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByLabel("搜索工单").fill(title);
  await expect(
    page.getByRole("button", { name: title, exact: true }),
  ).toBeVisible();
});

test("ticket export downloads CSV", async ({ page }) => {
  await login(page, "客户", "/tickets");
  await page.getByLabel("搜索工单").fill("TK-DEMO-001");
  const waiting = page.waitForEvent("download");
  await page.getByRole("button", { name: "导出筛选结果" }).click();
  const download = await waiting;
  expect(download.suggestedFilename()).toBe("tickets.csv");
  const content = await fs.readFile((await download.path())!, "utf8");
  expect(content).toContain("TK-DEMO-001");
  expect(content).toContain("工单编号");
});

test("administrator can submit and inspect a durable index job", async ({
  page,
}, info) => {
  await login(page, "管理员", "/knowledge");
  await page
    .getByRole("button", { name: "后台建立向量索引", exact: true })
    .click();
  await expect(
    page.locator(".index-jobs").getByText("等待处理").first(),
  ).toBeVisible();
  const result = await page.request.get("/api/knowledge/jobs");
  expect(result.ok()).toBeTruthy();
  expect((await result.json()).data[0].state).toBe("PENDING");
  await page.screenshot({
    path: info.outputPath("knowledge-jobs.png"),
    fullPage: true,
  });
});

test("agent cannot access product maintenance", async ({ page }) => {
  await login(page, "客服", "/products");
  await expect(page).toHaveURL(/\/overview$/);
  expect((await page.request.get("/api/admin/products")).status()).toBe(403);
});

test("browser navigation preserves the requested orders page", async ({
  page,
}) => {
  await login(page, "客户", "/orders?page=2&size=1");
  await expect(page.locator(".order-card")).toContainText("ZS20261030002");
  await page.getByRole("button", { name: "上一页" }).click();
  await expect(page.locator(".order-card")).toContainText("ZS20261030001");
  await page.goBack();
  await expect(page.locator(".order-card")).toContainText("ZS20261030002");
});

test("product create and update honor operation-specific validation", async ({
  page,
}, info) => {
  await login(page, "管理员", "/products");
  const name = "分组校验商品 " + Date.now();
  await page.getByRole("button", { name: "新增商品", exact: true }).click();
  await page.getByLabel("商品名称", { exact: true }).fill(name);
  await page.getByLabel("分类", { exact: true }).fill("学习");
  await page
    .getByLabel("商品描述", { exact: true })
    .fill("虚构商品，用于类型化 CRUD 验收");
  await page.getByLabel("价格（分）", { exact: true }).fill("12345");
  await page.getByRole("button", { name: "保存商品", exact: true }).click();
  const row = page.getByRole("row").filter({ hasText: name });
  await expect(row).toContainText("¥123.45");
  await row.getByRole("button", { name: "编辑", exact: true }).click();
  await page.getByLabel("价格（分）", { exact: true }).fill("23456");
  await page.getByRole("button", { name: "保存商品", exact: true }).click();
  await expect(row).toContainText("¥234.56");
  await page.reload();
  await expect(row).toContainText("¥234.56");
  await page.screenshot({
    path: info.outputPath("typed-product.png"),
    fullPage: true,
  });
});

test("typed SSE order result survives history reload", async ({
  page,
}, info) => {
  await login(page, "客户", "/chat");
  await page.getByLabel("输入咨询内容").fill("查询订单 ZS20261030001");
  await page.getByRole("button", { name: "发送咨询", exact: true }).click();
  await expect(page.locator(".message.assistant")).toContainText(
    "ZS20261030001",
  );
  const response = await page.request.get("/api/chat/threads");
  const thread = (await response.json()).data[0];
  const messages = (
    await (
      await page.request.get(`/api/chat/threads/${thread.id}/messages`)
    ).json()
  ).data;
  expect(
    messages.find((message: { role: string }) => message.role === "assistant")
      .detail.orders[0].order_no,
  ).toBe("ZS20261030001");
  await page.reload();
  await page.getByRole("button", { name: /历史咨询/ }).click();
  await page
    .locator(".thread-list button")
    .filter({ hasText: "查询订单 ZS20261030001" })
    .first()
    .click();
  await expect(page.locator(".message.assistant")).toContainText(
    "ZS20261030001",
  );
  await page.screenshot({
    path: info.outputPath("typed-chat-history.png"),
    fullPage: true,
  });
});
