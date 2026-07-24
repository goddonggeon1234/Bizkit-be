package com.caro.bizkit.domain.auth.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.auth.dto.LoginRequest;
import com.caro.bizkit.domain.auth.dto.RefreshRequest;
import com.caro.bizkit.domain.auth.dto.TokenPair;
import com.caro.bizkit.domain.auth.service.AuthService;
import com.caro.bizkit.domain.auth.service.WsTicketService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;
    private final WsTicketService wsTicketService;

    public AuthController(AuthService authService, WsTicketService wsTicketService) {
        this.authService = authService;
        this.wsTicketService = wsTicketService;
    }

    @PostMapping("/login/{provider}")
    @Operation(summary = "로그인", description = "pathvariable로 소셜 서비스 회사명(kakao)를 받고 body로 " +
            "소셜 로그인 코드와 redirect_uri를 받아 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공"
            )
    })
    public ResponseEntity<ApiResponse<TokenPair>> login(
            @Parameter(description = "소셜 로그인 제공자", example = "kakao")
            @PathVariable String provider,
            @Valid @RequestBody LoginRequest request
    ) {
        TokenPair tokenPair = authService.login(provider, request.code(), request.redirectUri());
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenPair));
    }

    @PostMapping("/rotation")
    @Operation(summary = "토큰 갱신", description = "RefreshToken만으로 새로운 AccessToken과 RefreshToken을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공"
            )
    })
    public ResponseEntity<ApiResponse<TokenPair>> refresh(
            @RequestBody RefreshRequest refreshRequest
    ) {
        if (refreshRequest == null || refreshRequest.refreshToken() == null) {
            log.warn("토큰 재발행 실패: 리프레시 토큰 없음");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
        }

        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", authService.refresh(refreshRequest.refreshToken())));
    }


    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "RefreshToken을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        authService.logout(userPrincipal.id());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공", null));
    }

    @PostMapping("/ws-ticket")
    @Operation(summary = "WebSocket 티켓 발급", description = "WebSocket 연결을 위한 일회용 티켓을 발급합니다. (TTL 30초)")
    public ResponseEntity<ApiResponse<Map<String, String>>> issueWsTicket(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String ticket = wsTicketService.issueTicket(principal.id());
        return ResponseEntity.ok(ApiResponse.success("티켓 발급 성공", Map.of("ticket", ticket)));
    }

    @GetMapping("/kakao/callback")
    @Operation(summary = "카카오 콜백", description = "카카오 OAuth 콜백 테스트용 엔드포인트입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "콜백 수신"
            )
    })
    public ResponseEntity<String> kakaoCallback(@RequestParam String code) {
        return ResponseEntity.ok(code);
    }

}
