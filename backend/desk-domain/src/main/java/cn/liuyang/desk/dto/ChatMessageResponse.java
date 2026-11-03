package cn.liuyang.desk.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@tools.jackson.databind.annotation.JsonNaming(
    tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatMessageResponse {
  private String id;
  private String threadId;
  private String role;
  private String content;
  private String createdAt;
  private ChatAnswer detail;
}
