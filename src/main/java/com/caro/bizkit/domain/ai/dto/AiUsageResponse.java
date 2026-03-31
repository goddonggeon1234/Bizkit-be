package com.caro.bizkit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiUsageResponse(
        @JsonProperty("weekly_count") Integer weeklyCount,
        @JsonProperty("total_count") Integer totalCount
) {}
