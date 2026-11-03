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
@TableName("index_jobs")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndexTask {
  @TableId(type = IdType.INPUT)
  private String id;

  private String documentId;
  private String actorId;
  @com.fasterxml.jackson.annotation.JsonIgnore private String requestKey;
  private String state;
  private int attempts;
  private int indexed;
  private int total;
  private long availableAt;
  private long leaseUntil;
  @com.fasterxml.jackson.annotation.JsonIgnore private String leaseToken;
  private String errorMessage;
  private String createdAt;
  private String updatedAt;
}
