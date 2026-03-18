package com.caro.bizkit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiJobResultMessage(
        @JsonProperty("backend_task_id") Integer backendTaskId,
        @JsonProperty("ai_task_id") String aiTaskId,
        @JsonProperty("user_id") Integer userId,
        @JsonProperty("card_id") Integer cardId,
        String status,
        Data data,
        String error
) {
    public record Data(
            String introduction
    ) {}
}
