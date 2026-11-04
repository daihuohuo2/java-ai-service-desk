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
@TableName("products")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Product {
  @TableId(type = IdType.INPUT)
  private String id;

  private String name;
  private String category;
  private long priceCents;
  private String description;
  private boolean active;
}
