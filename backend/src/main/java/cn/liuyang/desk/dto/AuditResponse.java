package cn.liuyang.desk.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@tools.jackson.databind.annotation.JsonNaming(
    tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuditResponse {
  private String id;
  private String actorId;
  private String action;
  private String target;
  private String summary;
  private String createdAt;
  private String actorName;
}
