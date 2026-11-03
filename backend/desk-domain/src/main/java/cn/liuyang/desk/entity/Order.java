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
@TableName("shop_orders")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Order {
  @TableId(type = IdType.INPUT)
  private String id;

  private String orderNo;
  private String ownerId;
  private String productId;
  private int quantity;
  private long totalCents;
  private String status;
  private String createdAt;
  private String deliveredAt;
}
