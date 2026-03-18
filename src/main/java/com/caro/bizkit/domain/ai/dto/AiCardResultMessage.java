package com.caro.bizkit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiCardResultMessage(
        @JsonProperty("backend_task_id") Integer backendTaskId,
        @JsonProperty("ai_task_id") String aiTaskId,
        @JsonProperty("user_id") Integer userId,
        String status,
        Data data,
        String error
) {
    public record Data(
            @JsonProperty("image_base64") String imageBase64
    ) {}
}
