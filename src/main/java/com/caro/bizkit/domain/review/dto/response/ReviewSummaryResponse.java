package com.caro.bizkit.domain.review.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReviewSummaryResponse(
        @JsonProperty("review_count") Integer reviewCount,
        @JsonProperty("average_score") Double averageScore,
        @JsonProperty("calculated_score") Double calculatedScore,
        @JsonProperty("top_tags") List<TagCountResponse> topTags
) {}
