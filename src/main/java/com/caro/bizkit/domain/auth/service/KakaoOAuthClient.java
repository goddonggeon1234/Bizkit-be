package com.caro.bizkit.domain.auth.service;

import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.common.exception.KakaoOAuthException;
import com.caro.bizkit.domain.auth.dto.KakaoOAuthErrorResponse;
import com.caro.bizkit.domain.auth.dto.KakaoTokenResponse;
import com.caro.bizkit.domain.auth.dto.KakaoUnlinkResponse;
import com.caro.bizkit.domain.auth.dto.KakaoUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class KakaoOAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final KakaoOAuthProperties properties;
    private final RestClient restClient;

    public KakaoOAuthClient(KakaoOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public KakaoTokenResponse exchangeCodeForToken(String code, String redirectUri) {
        String clientId = properties.getClientId();
        String clientSecret = properties.getClientSecret();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret) || !StringUtils.hasText(redirectUri)) {
            log.error("Kakao OAuth config missing. clientId={}, redirectUri={}", clientId, redirectUri);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Kakao OAuth 설정이 누락되었습니다.");
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        return restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        KakaoOAuthErrorResponse error = response.bodyTo(KakaoOAuthErrorResponse.class);
                        if (error == null) error = new KakaoOAuthErrorResponse(null, null, null, null, null);
                        throw toKakaoOAuthException("인가 코드가 맞지 않습니다.", response.getStatusCode(), error);
                    }
                    return response.bodyTo(KakaoTokenResponse.class);
                });
    }

    public KakaoUserResponse fetchUser(String accessToken) {
        return restClient.get()
                .uri(USER_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        KakaoOAuthErrorResponse error = response.bodyTo(KakaoOAuthErrorResponse.class);
                        if (error == null) error = new KakaoOAuthErrorResponse(null, null, null, null, null);
                        throw toKakaoOAuthException("카카오 사용자 정보 조회에 실패했습니다.", response.getStatusCode(), error);
                    }
                    return response.bodyTo(KakaoUserResponse.class);
                });
    }

    public KakaoUnlinkResponse unlinkUserByAdminKey(String adminKey, String kakaoUserId) {
        if (!StringUtils.hasText(adminKey)) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Kakao Admin Key 설정이 누락되었습니다.");
        }
        if (!StringUtils.hasText(kakaoUserId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Kakao user id가 없습니다.");
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("target_id_type", "user_id");
        form.add("target_id", kakaoUserId);

        return restClient.post()
                .uri(UNLINK_URL)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        KakaoOAuthErrorResponse error = response.bodyTo(KakaoOAuthErrorResponse.class);
                        if (error == null) error = new KakaoOAuthErrorResponse(null, null, null, null, null);
                        throw toKakaoOAuthException("카카오 연결 해제에 실패했습니다.", response.getStatusCode(), error);
                    }
                    return response.bodyTo(KakaoUnlinkResponse.class);
                });
    }

    private KakaoOAuthException toKakaoOAuthException(
            String userMessage,
            HttpStatusCode statusCode,
            KakaoOAuthErrorResponse error
    ) {
        String apiMessage = "Kakao OAuth 요청에 실패했습니다.";

        String errorType = error.error();
        String errorCode = error.errorCode();
        String errorDescription = error.errorDescription();
        String msg = error.msg();
        Integer code = error.code();

        if (StringUtils.hasText(errorType) || StringUtils.hasText(errorDescription) || StringUtils.hasText(errorCode)) {
            StringBuilder builder = new StringBuilder("Kakao OAuth error");
            if (StringUtils.hasText(errorType)) builder.append(": ").append(errorType);
            if (StringUtils.hasText(errorCode)) builder.append(" (").append(errorCode).append(")");
            if (StringUtils.hasText(errorDescription)) builder.append(" - ").append(errorDescription);
            apiMessage = builder.toString();
        } else if (StringUtils.hasText(msg) || code != null) {
            StringBuilder builder = new StringBuilder("Kakao API error");
            if (code != null) builder.append(" (").append(code).append(")");
            if (StringUtils.hasText(msg)) builder.append(": ").append(msg);
            apiMessage = builder.toString();
        }

        return new KakaoOAuthException(userMessage, statusCode, apiMessage);
    }
}
