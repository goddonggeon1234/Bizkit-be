package com.caro.bizkit.common.S3.dto;

public record PresignedUrlResponse(
        String url,
        String key,
        long expiresInSeconds
) {
}
