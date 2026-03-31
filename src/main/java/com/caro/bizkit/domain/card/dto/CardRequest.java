package com.caro.bizkit.domain.card.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CardRequest(
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[가-힣a-zA-Z]+(\\.[가-힣a-zA-Z]+)?$", message = "이름 형식이 올바르지 않습니다")
        String name,
        @NotBlank
        @Email
        String email,
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
        String phone_number,
        @Size(max = 15)
        String lined_number,
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s()&.]+$", message = "회사명 형식이 올바르지 않습니다")
        String company,
        @Size(max = 20)
        String position,
        @Size(max = 20)
        String department,
        @NotNull
        LocalDate start_date,
        LocalDate end_date,
        Boolean is_progress,
        @Size(max = 500)
        String ai_image_key
) {
}
