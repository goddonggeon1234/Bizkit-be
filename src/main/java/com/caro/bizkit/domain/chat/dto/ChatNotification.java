package com.caro.bizkit.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatNotification(
        Integer room_id,
        int unread_count,
        String latest_message,
        LocalDateTime latest_message_created_at
) {
}
