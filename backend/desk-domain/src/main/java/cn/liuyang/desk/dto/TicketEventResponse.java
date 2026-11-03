package cn.liuyang.desk.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@tools.jackson.databind.annotation.JsonNaming(
    tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TicketEventResponse {
  private String id;
  private String ticketId;
  private String actorId;
  private String eventType;
  private String content;
  private String createdAt;
  private String actorName;
  private String actorRole;
}
