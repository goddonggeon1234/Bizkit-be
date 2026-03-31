package com.caro.bizkit.domain.auth.dto;

public record TokenPair(
        String accessToken,
        String refreshToken
) {}
