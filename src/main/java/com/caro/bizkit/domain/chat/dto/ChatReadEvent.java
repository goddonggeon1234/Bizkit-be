package com.caro.bizkit.domain.chat.dto;

public record ChatReadEvent(
        Integer room_id,
        Long last_read_message_id,
        Integer target_user_id
) {
}
