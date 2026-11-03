package cn.liuyang.desk.dto;

public record UserResponse(
    String id,
    String username,
    @com.fasterxml.jackson.annotation.JsonProperty("display_name") String displayName,
    String role) {}
