package com.caro.bizkit.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotNull Integer room_id,
        @NotBlank @Size(max = 2000) String content
) {
}
