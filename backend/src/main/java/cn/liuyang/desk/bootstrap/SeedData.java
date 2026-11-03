package cn.liuyang.desk.bootstrap;

import cn.liuyang.desk.repository.Store;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements ApplicationRunner {
  private final Store s;
  private final PasswordEncoder passwords;

  @Value("${desk.demo}")
  boolean demo;

  @Value("${desk.admin-password}")
  String admin;

  @Value("${desk.customer-password}")
  String customer;

  @Value("${desk.agent-password}")
  String agent;

  @Value("${desk.deployment-mode}")
  String mode;

  @Value("${server.servlet.session.cookie.secure}")
  boolean secure;

  public SeedData(Store s, PasswordEncoder passwords) {
    this.s = s;
    this.passwords = passwords;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!demo && (admin.startsWith("Demo") || admin.length() < 14))
      throw new IllegalStateException(
          "Set a unique ADMIN_PASSWORD of at least 14 characters outside demo mode");
    if ("production".equals(mode) && (demo || !secure))
      throw new IllegalStateException("Production requires APP_DEMO=false and COOKIE_SECURE=true");
    addUser("admin", "admin", "林悦", "ADMIN", admin);
    if (!demo) return;
    addUser("customer", "customer", "陈一诺", "CUSTOMER", customer);
    addUser("customer2", "customer2", "周予安", "CUSTOMER", customer);
    addUser("agent", "agent", "顾青 · 客服", "AGENT", agent);
    if (s.db.queryForObject("SELECT COUNT(*) FROM products", Integer.class) > 0) return;
    s.db.update(
        "INSERT INTO products VALUES(?,?,?,?,?,?)",
        "P100",
        "栖光阅读灯",
        "桌面照明",
        19900,
        "三档色温、触控调光，适合桌面阅读。",
        true);
    s.db.update(
        "INSERT INTO products VALUES(?,?,?,?,?,?)",
        "P200",
        "云屿保温杯",
        "日常好物",
        12900,
        "500 mL 不锈钢保温杯，杯盖可拆洗。",
        true);
    s.db.update(
        "INSERT INTO products VALUES(?,?,?,?,?,?)",
        "P300",
        "轻舟蓝牙耳机",
        "数码配件",
        29900,
        "支持蓝牙连接与通话，配备充电盒。",
        true);
    String created = Instant.now().minus(5, ChronoUnit.DAYS).toString(),
        delivered = Instant.now().minus(2, ChronoUnit.DAYS).toString();
    order("order-1", "ZS20261030001", "customer", "P100", 19900, "DELIVERED", created, delivered);
    order("order-2", "ZS20261030002", "customer", "P200", 12900, "SHIPPED", created, null);
    order("order-3", "ZS20261030003", "customer2", "P300", 29900, "DELIVERED", created, delivered);
    doc(
        "doc-refund",
        "售后与退换货规则",
        null,
        "签收后 7 天内，商品未使用且包装、配件完好，可申请七天无理由退货。定制商品和已拆封的个人卫生类商品不适用。是否通过以人工审核为准。\n\n"
            + "商品签收后出现质量问题，可申请维修或换货。请提供订单号和问题描述，由客服核实；系统提交申请不代表已经批准退款或完成退款。\n\n"
            + "退款申请通过且退回商品验收完成后，退款会在 3 至 7 个工作日内原路退回。物流费用根据问题原因和人工审核结果确认，不能预先承诺免费。");
    doc(
        "doc-shipping",
        "发货与物流说明",
        null,
        "普通订单付款后通常在 48 小时内发货，节假日或预售商品以订单页面说明为准。运输时间受地区、天气和承运商影响。\n\n"
            + "已发货订单如需取消或修改地址，请提交人工工单。客服核实物流状态后处理，不能保证拦截成功。");
    doc(
        "doc-lamp",
        "栖光阅读灯使用说明",
        "P100",
        "栖光阅读灯需要连接电源使用，不含内置电池，不支持无线充电。使用额定 5V/2A 电源适配器。\n\n"
            + "栖光阅读灯支持三档色温和触控调光。长按触控区域调节亮度，短按切换开关。设备无 App 连接功能。\n\n"
            + "栖光阅读灯自签收日起提供一年有限保修。人为损坏、进水和私自拆机不属于免费保修范围；故障请先联系人工客服。");
    doc(
        "doc-cup",
        "云屿保温杯使用说明",
        "P200",
        "云屿保温杯容量为 500 mL，内胆为 304 不锈钢。不可放入微波炉，不建议盛放碳酸饮料。\n\n"
            + "杯盖可拆卸清洗，建议使用中性清洁剂。保温效果受环境温度、装水量和开盖频率影响，资料没有承诺固定保温小时数。");
    doc(
        "doc-earbuds",
        "轻舟蓝牙耳机使用说明",
        "P300",
        "轻舟蓝牙耳机支持蓝牙连接和语音通话，充电盒使用 USB-C 接口。首次连接时打开充电盒并在手机蓝牙列表中选择轻舟耳机。\n\n"
            + "当前资料未提供防水等级、降噪等级和续航小时数，请勿据此推断；如连接异常，可将耳机放回充电盒后重新配对。");
    String t = "ticket-demo", now = Store.now();
    s.db.update(
        "INSERT INTO"
            + " tickets(id,ticket_no,owner_id,order_id,kind,subject,description,status,assignee_id,request_key,request_hash,version,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        t,
        "TK-DEMO-001",
        "customer",
        "order-2",
        "QUESTION",
        "想确认保温杯的物流进度",
        "包裹已经发出，想了解预计送达时间。",
        "IN_PROGRESS",
        "agent",
        "seed-ticket",
        "seed",
        1,
        now,
        now);
    s.db.update(
        "INSERT INTO ticket_events VALUES(?,?,?,?,?,?)",
        Store.id(),
        t,
        "customer",
        "CREATED",
        "包裹已经发出，想了解预计送达时间。",
        now);
    s.db.update(
        "INSERT INTO ticket_events VALUES(?,?,?,?,?,?)",
        Store.id(),
        t,
        "agent",
        "REPLY",
        "您好，已为您登记物流咨询。运输时效会受地区和天气影响，我们正在核实。",
        now);
  }

  private void addUser(String id, String name, String display, String role, String pass) {
    if (s.db.queryForObject("SELECT COUNT(*) FROM app_users WHERE username=?", Integer.class, name)
        == 0)
      s.db.update(
          "INSERT INTO app_users VALUES(?,?,?,?,?)",
          id,
          name,
          display,
          passwords.encode(pass),
          role);
  }

  private void order(
      String id,
      String no,
      String owner,
      String product,
      long cents,
      String status,
      String at,
      String delivered) {
    s.db.update(
        "INSERT INTO shop_orders VALUES(?,?,?,?,?,?,?,?,?)",
        id,
        no,
        owner,
        product,
        1,
        cents,
        status,
        at,
        delivered);
  }

  private void doc(String id, String title, String product, String content) {
    s.db.update(
        "INSERT INTO knowledge_documents VALUES(?,?,?,?,?,?)",
        id,
        title,
        product,
        1,
        true,
        Store.now());
    int n = 0;
    for (String para : content.split("\n\n"))
      s.db.update(
          "INSERT INTO knowledge_chunks VALUES(?,?,?,?,?,?)",
          Store.id(),
          id,
          ++n,
          para,
          null,
          null);
  }
}
