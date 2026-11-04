package cn.liuyang.desk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Database row; joins are explicitly marked as non-persistent fields. */
@Getter
@Setter
@NoArgsConstructor
@TableName("chat_messages")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatMessage {
  @TableId(type = IdType.INPUT)
  private String id;

  private String threadId;
  private String role;
  private String content;
  @com.fasterxml.jackson.annotation.JsonIgnore private String detailJson;
  private String createdAt;
}
