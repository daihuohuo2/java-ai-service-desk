package cn.liuyang.desk.domain;

import static org.junit.jupiter.api.Assertions.*;

import cn.liuyang.desk.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketPolicyTest {
  @ParameterizedTest
  @CsvSource({
    "OPEN,IN_PROGRESS",
    "IN_PROGRESS,WAITING_CUSTOMER",
    "IN_PROGRESS,RESOLVED",
    "WAITING_CUSTOMER,IN_PROGRESS",
    "WAITING_CUSTOMER,RESOLVED",
    "RESOLVED,IN_PROGRESS",
    "RESOLVED,CLOSED"
  })
  void staffMayFollowDocumentedTransitions(String current, String target) {
    assertEquals(
        target, TicketPolicy.nextStatus("customer", 3, current, "agent", true, 3, target, "处理说明"));
  }

  @Test
  void staleVersionPreventsLostUpdate() {
    assertEquals(
        409,
        assertThrows(
                ApiException.class,
                () ->
                    TicketPolicy.nextStatus(
                        "customer", 4, "IN_PROGRESS", "agent", true, 3, "RESOLVED", "处理说明"))
            .status);
  }

  @Test
  void foreignCustomerGetsNotFoundBeforeVersionInformation() {
    assertEquals(
        404,
        assertThrows(
                ApiException.class,
                () ->
                    TicketPolicy.nextStatus(
                        "customer", 4, "OPEN", "customer2", false, 3, null, "回复"))
            .status);
  }

  @Test
  void customerCannotResolveTheirOwnTicket() {
    assertEquals(
        403,
        assertThrows(
                ApiException.class,
                () ->
                    TicketPolicy.nextStatus(
                        "customer", 0, "IN_PROGRESS", "customer", false, 0, "RESOLVED", "批准退款"))
            .status);
  }

  @Test
  void customerReplyReopensWaitingTicket() {
    assertEquals(
        "IN_PROGRESS",
        TicketPolicy.nextStatus(
            "customer", 2, "WAITING_CUSTOMER", "customer", false, 2, null, "补充材料"));
  }

  @Test
  void resolutionRequiresExplanation() {
    assertEquals(
        400,
        assertThrows(
                ApiException.class,
                () ->
                    TicketPolicy.nextStatus(
                        "customer", 0, "IN_PROGRESS", "agent", true, 0, "RESOLVED", " "))
            .status);
  }

  @Test
  void closedTicketRejectsFurtherReplies() {
    assertEquals(
        409,
        assertThrows(
                ApiException.class,
                () ->
                    TicketPolicy.nextStatus("customer", 5, "CLOSED", "agent", true, 5, null, "回复"))
            .status);
  }
}
