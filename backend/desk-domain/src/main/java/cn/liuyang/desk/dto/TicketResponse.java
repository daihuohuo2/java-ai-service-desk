package cn.liuyang.desk.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@tools.jackson.databind.annotation.JsonNaming(
    tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TicketResponse {
  private String id;
  private String ticketNo;
  private String ownerId;
  private String orderId;
  private String kind;
  private String subject;
  private String description;
  private String status;
  private String assigneeId;
  private String createdAt;
  private String updatedAt;
  private String customerName;
  private String productName;
  private String orderNo;
  private String assigneeName;
  private int version;
  private java.util.List<TicketEventResponse> events;
}
