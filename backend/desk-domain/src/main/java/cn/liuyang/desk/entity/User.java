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
@TableName("app_users")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class User {
  @TableId(type = IdType.INPUT)
  private String id;

  private String username;
  @com.fasterxml.jackson.annotation.JsonIgnore private String passwordHash;
  private String displayName;
  private String role;
}
