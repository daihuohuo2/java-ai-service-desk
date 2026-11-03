package cn.liuyang.desk.controller;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiAdvice {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<?> api(ApiException e) {
    return ResponseEntity.status(e.status).body(ApiResponse.failure(e.status, e.getMessage()));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> invalid(Exception e) {
    return ResponseEntity.badRequest().body(ApiResponse.failure(400, "输入格式不正确，请检查必填项和长度"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> validation(MethodArgumentNotValidException exception) {
    Map<String, String> fields = new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors().forEach(error ->
        fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.badRequest().body(new ApiResponse<>(400, "输入格式不正确，请检查必填项和长度", fields));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<?> large(Exception e) {
    return ResponseEntity.status(413).body(ApiResponse.failure(413, "文件不能超过 256 KB"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict(Exception e) {
    return ResponseEntity.status(409).body(ApiResponse.failure(409, "记录已变化或存在重复操作，请刷新后重试"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    LoggerFactory.getLogger(getClass())
        .error("Unhandled request error: {}", e.getClass().getSimpleName());
    return ResponseEntity.internalServerError().body(ApiResponse.failure(500, "操作未完成，请稍后重试"));
  }
}
