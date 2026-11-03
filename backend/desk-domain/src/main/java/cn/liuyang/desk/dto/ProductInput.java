package cn.liuyang.desk.dto;

import cn.liuyang.desk.common.validation.ValidationGroups;
import jakarta.validation.constraints.*;

public record ProductInput(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 80) String category,
    @Min(0) @Max(100000000) long priceCents,
    @NotBlank @Size(max = 2000) String description,
    boolean active,
    @Null(groups = ValidationGroups.Create.class, message = "新增时由服务端生成编号")
        @NotBlank(groups = ValidationGroups.Update.class, message = "修改时必须携带商品编号")
        @Size(max = 40)
        String id) {
  public ProductInput(
      String name, String category, long priceCents, String description, boolean active) {
    this(name, category, priceCents, description, active, null);
  }
}
