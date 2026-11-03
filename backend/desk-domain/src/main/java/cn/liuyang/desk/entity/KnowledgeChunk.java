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
@TableName("knowledge_chunks")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KnowledgeChunk {
  @TableId(type = IdType.INPUT)
  private String id;

  private String documentId;
  private int ordinal;
  private String content;
  @com.fasterxml.jackson.annotation.JsonIgnore private String embedding;
  @com.fasterxml.jackson.annotation.JsonIgnore private String embeddingModel;

  @TableField(exist = false)
  private String title;

  @TableField(exist = false)
  private String productId;

  @TableField(exist = false)
  private int version;
}
