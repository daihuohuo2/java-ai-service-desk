package cn.liuyang.desk.domain;

import cn.liuyang.desk.common.ApiException;
import java.util.*;

/** Pure state transition rules. No Spring context, database or clock is needed. */
public final class TicketPolicy {
  private TicketPolicy() {}

  private static final Map<String, Set<String>> TRANSITIONS =
      Map.of(
          "OPEN",
          Set.of("IN_PROGRESS"),
          "IN_PROGRESS",
          Set.of("WAITING_CUSTOMER", "RESOLVED"),
          "WAITING_CUSTOMER",
          Set.of("IN_PROGRESS", "RESOLVED"),
          "RESOLVED",
          Set.of("IN_PROGRESS", "CLOSED"),
          "CLOSED",
          Set.of());

  public static String nextStatus(
      String ownerId,
      int storedVersion,
      String currentStatus,
      String actor,
      boolean staff,
      int expectedVersion,
      String status,
      String message) {
    if (!staff && !actor.equals(ownerId)) {
      throw ApiException.missing();
    }
    if (storedVersion != expectedVersion) {
      throw new ApiException(409, "工单已被更新，请刷新后重试");
    }
    String old = currentStatus;
    String next = status == null || status.isBlank() ? old : status;
    if ("CLOSED".equals(old)) {
      throw new ApiException(409, "工单已关闭，无法继续修改");
    }
    if (!staff && !next.equals(old) && !("RESOLVED".equals(old) && "CLOSED".equals(next))) {
      throw new ApiException(403, "客户只能确认关闭已解决的工单");
    }
    if (!next.equals(old) && !TRANSITIONS.get(old).contains(next)) {
      throw new ApiException(409, "不允许的工单状态变化");
    }
    if ((message == null || message.isBlank()) && next.equals(old)) {
      throw new ApiException(400, "请输入回复或选择新的状态");
    }
    if (("RESOLVED".equals(next) || "WAITING_CUSTOMER".equals(next))
        && !next.equals(old)
        && (message == null || message.isBlank())) {
      throw new ApiException(400, "请说明处理结果或需要补充的材料");
    }
    if (!staff && "WAITING_CUSTOMER".equals(old) && message != null && !message.isBlank()) {
      next = "IN_PROGRESS";
    }

    return next;
  }
}
