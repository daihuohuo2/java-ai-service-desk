package cn.liuyang.desk;

import jakarta.validation.constraints.*;

public record ProductInput(@NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String category, @Min(0) @Max(100000000) long priceCents,
        @NotBlank @Size(max = 2000) String description, boolean active) {
}
