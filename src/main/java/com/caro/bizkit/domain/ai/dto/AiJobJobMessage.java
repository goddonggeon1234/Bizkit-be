package com.caro.bizkit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiJobJobMessage(
        @JsonProperty("backend_task_id") Integer backendTaskId,
        @JsonProperty("user_id") Integer userId,
        @JsonProperty("card_id") Integer cardId,
        String name,
        String company,
        String department,
        String position,
        List<AiJobAnalyzeRequest.ProjectDto> projects,
        List<AiJobAnalyzeRequest.AwardDto> awards
) {}
