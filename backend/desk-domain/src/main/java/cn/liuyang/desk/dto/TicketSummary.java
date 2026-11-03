package cn.liuyang.desk.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@tools.jackson.databind.annotation.JsonNaming(
    tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TicketSummary {
  private String id;
  private String ticketNo;
  private String subject;
  private String kind;
  private String status;
  private String updatedAt;
  private String customerName;
}
