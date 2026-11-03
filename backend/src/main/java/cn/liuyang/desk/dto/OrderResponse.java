package cn.liuyang.desk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** SQL -> Java record -> JSON, preserving the original API field names. */
public record OrderResponse(
    String id,
    @JsonProperty("order_no") String orderNo,
    @JsonProperty("owner_id") String ownerId,
    @JsonProperty("product_id") String productId,
    int quantity,
    @JsonProperty("total_cents") long totalCents,
    String status,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("delivered_at") String deliveredAt,
    @JsonProperty("product_name") String productName,
    String category) {}
