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
@TableName("tickets")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Ticket {
  @TableId(type = IdType.INPUT)
  private String id;

  private String ticketNo;
  private String ownerId;
  private String orderId;
  private String kind;
  private String subject;
  private String description;
  private String status;
  private String assigneeId;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestKey;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestHash;
  private int version;
  private String createdAt;
  private String updatedAt;

  @TableField(exist = false)
  private String customerName;

  @TableField(exist = false)
  private String productName;

  @TableField(exist = false)
  private String orderNo;

  @TableField(exist = false)
  private String assigneeName;
}
