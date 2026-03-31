package com.caro.bizkit.domain.userdetail.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ActivityRequest(
        @NotBlank
        @Size(max = 50)
        String name,
        @Size(max = 50)
        String grade,
        @NotBlank
        @Size(max = 50)
        String organization,
        @Size(max = 2000)
        String content,
        @NotNull
        LocalDate win_date
) {
}
