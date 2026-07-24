package com.caro.bizkit.domain.auth.dto;

public record LoginResponse(
        String provider,
        String providerId,
        String email,
        String nickname,
        String image_url
) {
}
