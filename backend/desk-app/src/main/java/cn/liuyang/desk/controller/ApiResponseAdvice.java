package cn.liuyang.desk.controller;

import cn.liuyang.desk.common.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Wrap JSON once. CSV downloads, SSE events and Actuator retain their own protocols. */
@RestControllerAdvice(basePackages = "cn.liuyang.desk.controller")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {
  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return !SseEmitter.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType contentType,
      Class<? extends HttpMessageConverter<?>> converterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body == null
        || body instanceof ApiResponse<?>
        || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
      return body;
    }
    return ApiResponse.success(body);
  }
}
