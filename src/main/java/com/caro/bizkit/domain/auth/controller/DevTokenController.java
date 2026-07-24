package com.caro.bizkit.domain.auth.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.auth.dto.TokenPair;
import com.caro.bizkit.security.JwtTokenProvider;
import com.caro.bizkit.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Tag(name = "Dev", description = "개발 환경 전용 API (부하테스트용)")
public class DevTokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/token")
    @Operation(summary = "테스트용 토큰 발급", description = "부하테스트용 임시 액세스/리프레시 토큰을 발급합니다. dev 프로파일에서만 동작합니다.")
    public ResponseEntity<ApiResponse<TokenPair>> issueToken(
            @RequestParam Integer userId
    ) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                String.valueOf(userId), Map.of()
        );
        String refreshToken = refreshTokenService.createRefreshToken(userId);

        return ResponseEntity.ok(
                ApiResponse.success("테스트 토큰 발급 성공", new TokenPair(accessToken, refreshToken))
        );
    }
}
