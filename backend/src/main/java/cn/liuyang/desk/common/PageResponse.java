package cn.liuyang.desk.common;

import java.util.List;

/** A bounded page; page numbers start at 1. */
public record PageResponse<T>(List<T> items, long total, int page, int size, long pages) {
  public static void validate(int page, int size) {
    if (page < 1 || page > 1000000 || size < 1 || size > 100) {
      throw new ApiException(400, "页码须为 1–1000000，每页须为 1–100 条");
    }
  }

  public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
    return new PageResponse<>(List.copyOf(items), total, page, size, (total + size - 1) / size);
  }
}
