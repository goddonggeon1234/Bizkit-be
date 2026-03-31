package com.caro.bizkit.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String code,
        @NotBlank String redirectUri
) {
}
