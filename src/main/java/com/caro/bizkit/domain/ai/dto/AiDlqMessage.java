package com.caro.bizkit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jobs DLQ (TTL 만료) 및 Result DLQ (nack 초과) 공통 메시지.
 * 두 큐 모두 backend_task_id, user_id를 포함한다.
 */
public record AiDlqMessage(
        @JsonProperty("backend_task_id") Integer backendTaskId,
        @JsonProperty("user_id") Integer userId
) {}
