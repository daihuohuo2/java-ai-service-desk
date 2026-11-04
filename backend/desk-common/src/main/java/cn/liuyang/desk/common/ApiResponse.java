package cn.liuyang.desk.common;

/** HTTP status indicates transport outcome; code is 0 for success and HTTP error code otherwise. */
public record ApiResponse<T>(int code, String message, T data) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(0, "成功", data);
  }

  public static ApiResponse<Void> failure(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
