package cn.liuyang.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductResponse(
    String id,
    String name,
    String category,
    @JsonProperty("price_cents") long priceCents,
    String description,
    boolean active) {}
