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
@TableName("knowledge_documents")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KnowledgeDocument {
  @TableId(type = IdType.INPUT)
  private String id;

  private String title;
  private String productId;
  private int version;
  private boolean active;
  private String createdAt;

  @TableField(exist = false)
  private long chunks;

  @TableField(exist = false)
  private long indexed;
}
