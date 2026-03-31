package com.caro.bizkit.domain.user.dto;

public record UserResponse(
        Integer id,
        String name,
        String email,
        String profile_image_url
) {
    public static UserResponse fromPrincipal(UserPrincipal principal, String profileImageUrl) {
        if (principal == null) {
            return null;
        }
        return new UserResponse(
                principal.id(),
                principal.name(),
                principal.email(),
                profileImageUrl
        );
    }
}
