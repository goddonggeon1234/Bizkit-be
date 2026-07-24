package com.caro.bizkit.domain.chat.dto;

import java.util.List;

public record ChatMessagesResult(
        List<ChatMessageResponse> messages,
        Long other_last_read_message_id
) {
}
