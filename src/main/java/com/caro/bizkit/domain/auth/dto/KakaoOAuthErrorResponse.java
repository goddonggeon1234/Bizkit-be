package com.caro.bizkit.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoOAuthErrorResponse(
        // kauth.kakao.com (토큰 API) 에러 응답 필드
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription,
        @JsonProperty("error_code") String errorCode,
        // kapi.kakao.com (사용자 API) 에러 응답 필드
        @JsonProperty("msg") String msg,
        @JsonProperty("code") Integer code
) {
}
