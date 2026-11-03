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
@TableName("chat_runs")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatRun {
  @TableId(type = IdType.INPUT)
  private String id;

  private String ownerId;
  private String threadId;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestKey;
  private String question;
  private String state;
  @com.fasterxml.jackson.annotation.JsonIgnore private String resultJson;
  private String modelMode;
  private Long latencyMs;
  private String createdAt;
}
