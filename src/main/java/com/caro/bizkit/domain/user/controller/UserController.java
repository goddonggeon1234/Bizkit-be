package com.caro.bizkit.domain.user.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.dto.UserResponse;

import com.caro.bizkit.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 정보 조회 API")
public class UserController {

    private final UserService userService;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public UserController(
            UserService userService,
            @Value("${cookie.secure:true}") boolean cookieSecure,
            @Value("${cookie.same-site:Lax}") String cookieSameSite
    ) {
        this.userService = userService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;

    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "인증된 사용자의 기본 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getMyStatus(@AuthenticationPrincipal UserPrincipal user) {
        UserResponse userResponse =  userService.getMyStatus(user);
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 성공", userResponse));
    }

    @GetMapping("/{user_id}")
    @Operation(summary = "상대 정보 조회", description = "수집한 카드의 주인 프로필을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수집한 카드의 주인이 아닌 경우")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("user_id") Integer userId
    ) {
        UserResponse userResponse = userService.getUserProfile(principal, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", userResponse));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "내 사용자 정보를 수정합니다.")
    public ResponseEntity<?> updateMyStatus(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody Map<String, Object> request
    ) {
        UserResponse userResponse = userService.updateMyStatus(user, request);
        return ResponseEntity.ok(ApiResponse.success("내 정보 수정 성공", userResponse));
    }


    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "카카오 연결 해제 후 계정을 탈퇴 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal user,
            HttpServletResponse response
    ) {
        userService.withdraw(user);
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴 성공", null));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie deleteAccess = buildCookie("accessToken", "", 0);
        ResponseCookie deleteRefresh = buildCookie("refreshToken", "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
