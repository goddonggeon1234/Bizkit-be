package com.caro.bizkit.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            @JsonProperty("email") String email,
            @JsonProperty("profile") KakaoProfile profile
    ) {
    }

    public record KakaoProfile(
            @JsonProperty("nickname") String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl) {
    }
}
