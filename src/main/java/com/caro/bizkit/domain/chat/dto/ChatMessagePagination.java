package com.caro.bizkit.domain.chat.dto;

public record ChatMessagePagination(
        Long cursorId,
        boolean has_next
) {
}
