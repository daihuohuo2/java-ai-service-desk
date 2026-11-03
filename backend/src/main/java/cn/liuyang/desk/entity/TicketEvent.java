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
@TableName("ticket_events")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TicketEvent {
  @TableId(type = IdType.INPUT)
  private String id;

  private String ticketId;
  private String actorId;
  private String type;
  private String content;
  private String createdAt;

  @TableField(exist = false)
  private String actorName;

  @TableField(exist = false)
  private String actorRole;
}
