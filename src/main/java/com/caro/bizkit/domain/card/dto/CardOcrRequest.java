package com.caro.bizkit.domain.card.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CardOcrRequest(
        @NotBlank
        @Size(max = 30)
        String name,
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(max = 255)
        String company,
        @Size(max = 100)
        String position,
        @Size(max = 255)
        String department,
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
        String phone_number,
        @Size(max = 15)
        String lined_number
) {
}
