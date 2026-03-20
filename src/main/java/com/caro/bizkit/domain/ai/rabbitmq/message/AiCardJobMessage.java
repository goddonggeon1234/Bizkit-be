package com.caro.bizkit.domain.ai.rabbitmq.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiCardJobMessage(
        @JsonProperty("backend_task_id") Integer backendTaskId,
        @JsonProperty("user_id") Integer userId,
        @JsonProperty("card_id") Integer cardId,
        String tag,
        String text,
        String name,
        String company,
        String department,
        String position,
        @JsonProperty("phone_number") String phoneNumber,
        String email
) {}
