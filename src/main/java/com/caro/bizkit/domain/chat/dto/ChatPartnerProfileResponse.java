package com.caro.bizkit.domain.chat.dto;

import com.caro.bizkit.domain.user.entity.User;

public record ChatPartnerProfileResponse(
        Integer id,
        String name,
        String company,
        String department,
        String position,
        String profile_image_url,
        String description
) {
    public static ChatPartnerProfileResponse from(User user, String profileImageUrl) {
        return new ChatPartnerProfileResponse(
                user.getId(),
                user.getName(),
                user.getCompany(),
                user.getDepartment(),
                user.getPosition(),
                profileImageUrl,
                user.getDescription()
        );
    }
}
