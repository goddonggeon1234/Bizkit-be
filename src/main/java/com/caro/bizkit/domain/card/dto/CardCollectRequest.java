package com.caro.bizkit.domain.card.dto;

import jakarta.validation.constraints.NotBlank;

public record CardCollectRequest(
        @NotBlank
        String uuid
) {
}
