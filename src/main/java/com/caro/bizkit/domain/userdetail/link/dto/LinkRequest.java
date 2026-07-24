package com.caro.bizkit.domain.userdetail.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkRequest(
        @Size(max = 100)
        String title,
        @NotBlank
        @Size(max = 2048)
        String link
) {
}
