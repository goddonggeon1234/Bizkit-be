package com.caro.bizkit.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record UserPrincipal(
        Integer id,
        String name,
        String email,
        String phone_number,
        String lined_number,
        String company,
        String department,
        String position,
        String profile_image_key,
        String description
) {
}
