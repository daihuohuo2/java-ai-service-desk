package cn.liuyang.desk;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductResponse(String id, String name, String category,
        @JsonProperty("price_cents") long priceCents, String description, boolean active) {
}
