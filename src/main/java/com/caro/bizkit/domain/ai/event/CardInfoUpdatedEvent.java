package com.caro.bizkit.domain.ai.event;

import java.time.LocalDateTime;

public record CardInfoUpdatedEvent(
        Integer resourceId,
        String updateType,
        LocalDateTime occurredAt
) {}
