package com.caro.bizkit.domain.userdetail.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank
        @Size(max = 100)
        String name,
        @Size(max = 2000)
        String content,
        @NotNull
        LocalDate start_date,
        LocalDate end_date,
        Boolean is_progress

) {
}
