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
@TableName("proposals")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Proposal {
  @TableId(type = IdType.INPUT)
  private String id;

  private String ownerId;
  private String orderId;
  private String kind;
  private String subject;
  private String description;
  private String expiresAt;
  private String ticketId;
  private String createdAt;
}
