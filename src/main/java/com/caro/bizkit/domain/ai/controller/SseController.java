package com.caro.bizkit.domain.ai.controller;

import com.caro.bizkit.domain.ai.service.SseEmitterService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "SSE", description = "Server-Sent Events API")
public class SseController {

    private final SseEmitterService sseEmitterService;

    @Operation(summary = "AI 명함 생성 SSE 연결", description = "AI 명함 이미지 생성 진행 상태를 실시간으로 수신합니다.")
    @GetMapping(value = "/ai-cards", produces = "text/event-stream")
    public SseEmitter connect(@AuthenticationPrincipal UserPrincipal user) {
        return sseEmitterService.connect(user.id());
    }
}
