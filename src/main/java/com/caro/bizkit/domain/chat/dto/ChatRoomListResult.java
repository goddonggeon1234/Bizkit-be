package com.caro.bizkit.domain.chat.dto;

import java.util.List;

public record ChatRoomListResult(
        List<ChatRoomResponse> data,
        Integer cursorId,
        boolean hasNext
) {
}
