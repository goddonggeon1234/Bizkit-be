package com.caro.bizkit.domain.chat.dto;

public record ChatReadNotification(
        Integer room_id,
        Long last_read_message_id
) {
}
