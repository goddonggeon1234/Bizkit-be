package com.caro.bizkit.domain.ai.rabbitmq.message;

import com.caro.bizkit.domain.ai.dto.AiHexAnalyzeRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AiHexJobMessage(
        @JsonProperty("backend_task_id") Integer backendTaskId,
        @JsonProperty("user_id") Integer userId,
        @JsonProperty("github_username") String githubUsername,
        AiHexAnalyzeRequest.Capabilities capabilities,
        AiHexAnalyzeRequest.Reviews reviews
) {}
