package com.caro.bizkit.domain.review.dto.response;

public record TagCountResponse(
        Integer id,
        String keyword,
        Long count
) {}
