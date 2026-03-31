package com.caro.bizkit.domain.ai.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.ai.dto.AiCardGenerationRequest;
import com.caro.bizkit.domain.ai.dto.AiUsageResponse;
import com.caro.bizkit.domain.ai.service.AiCardGenerationService;
import com.caro.bizkit.domain.ai.service.AiUsageService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/cards")
@RequiredArgsConstructor
@Validated
@Tag(name = "AI Card", description = "AI 명함 이미지 생성 API")
public class AiCardController {

    private final AiCardGenerationService aiCardGenerationService;
    private final AiUsageService aiUsageService;

    @Operation(summary = "AI 명함 이미지 생성 요청", description = "명함 정보를 기반으로 AI 이미지를 비동기 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> generateCardImage(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody AiCardGenerationRequest request
    ) {
        aiCardGenerationService.generate(user.id(), request.cardId(), request.tag(), request.text());
        return ResponseEntity.accepted().body(ApiResponse.success("AI 명함 이미지 생성 요청 성공", null));
    }

    @Operation(summary = "AI 사용량 조회", description = "주간 잔여 횟수 및 누적 생성 횟수를 반환합니다.")
    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<AiUsageResponse>> getUsage(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.success("AI 사용량 조회 성공", aiUsageService.getUsage(user.id())));
    }
}
