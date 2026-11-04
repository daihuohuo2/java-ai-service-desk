package cn.liuyang.desk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.dto.OrderResponse;
import cn.liuyang.desk.mapper.OrderMapper;
import cn.liuyang.desk.service.impl.OrderServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Only the service is real. There is no Spring context and no database. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
  @Mock OrderMapper mapper;
  @InjectMocks OrderServiceImpl service;

  @Test
  void pageEscapesWildcardsAndKeepsOwnerScope() {
    when(mapper.count("customer", "DELIVERED", "%!%!_!!%")).thenReturn(25L);
    when(mapper.page("customer", "DELIVERED", "%!%!_!!%", 10, 10L)).thenReturn(List.of());
    var page = service.page("customer", 2, 10, "DELIVERED", "%_!");
    assertEquals(3, page.pages());
    assertEquals(25, page.total());
    verify(mapper).page("customer", "DELIVERED", "%!%!_!!%", 10, 10L);
  }

  @Test
  void invalidPageStopsBeforeDatabaseAccess() {
    assertEquals(
        400,
        assertThrows(ApiException.class, () -> service.page("customer", 0, 10, null, null)).status);
    verifyNoInteractions(mapper);
  }

  @Test
  void missingOwnedOrderDoesNotFallBackToGlobalLookup() {
    when(mapper.owned("customer2", "order-1")).thenReturn(null);
    assertEquals(
        404, assertThrows(ApiException.class, () -> service.owned("customer2", "order-1")).status);
    verify(mapper).owned("customer2", "order-1");
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void shippedOrderAllowsQuestionButRejectsRefund() {
    var order =
        new OrderResponse(
            "id", "number", "customer", "p", 1, 100, "SHIPPED", "now", null, "name", "category");
    assertDoesNotThrow(() -> service.eligible(order, "QUESTION"));
    assertEquals(
        409, assertThrows(ApiException.class, () -> service.eligible(order, "REFUND")).status);
    verifyNoInteractions(mapper);
  }
}
