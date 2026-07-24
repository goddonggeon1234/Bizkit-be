package com.caro.bizkit.domain.chat.dto;

import com.caro.bizkit.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long message_id,
        Integer room_id,
        Integer sender_user_id,
        String sender_name,
        String content,
        LocalDateTime created_at
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getParticipant().getUser().getId(),
                message.getParticipant().getUser().getName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
