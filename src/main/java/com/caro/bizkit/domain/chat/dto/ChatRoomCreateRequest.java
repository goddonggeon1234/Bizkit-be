package com.caro.bizkit.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

public record ChatRoomCreateRequest(
        @NotNull Integer target_user_id
) {
}
