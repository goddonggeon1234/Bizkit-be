package com.caro.bizkit.domain.chat.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.chat.dto.ChatPartnerProfileResponse;
import com.caro.bizkit.domain.chat.service.ChatRoomService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/users")
@RequiredArgsConstructor
@Tag(name = "Chat User", description = "채팅 유저 API")
public class ChatUserController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/{user_id}")
    @Operation(summary = "채팅 상대 프로필 조회", description = "공통 채팅방이 있는 상대의 프로필을 조회합니다.")
    public ResponseEntity<ApiResponse<ChatPartnerProfileResponse>> getPartnerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("user_id") Integer userId
    ) {
        ChatPartnerProfileResponse response = chatRoomService.getPartnerProfile(principal, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", response));
    }
}
