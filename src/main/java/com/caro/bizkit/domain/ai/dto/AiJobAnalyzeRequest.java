package com.caro.bizkit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiJobAnalyzeRequest(
        //ai 서버에 request 형식을 맞추기 위한 것
        @JsonProperty("user_id") Integer cardId,
        String name,
        String company,
        String department,
        String position,
        List<ProjectDto> projects,
        List<AwardDto> awards
) {
    public record ProjectDto(
            String name,
            String content,
            @JsonProperty("period_months") Integer periodMonths
    ) {}

    public record AwardDto(
            String name,
            Integer year
    ) {}

}
