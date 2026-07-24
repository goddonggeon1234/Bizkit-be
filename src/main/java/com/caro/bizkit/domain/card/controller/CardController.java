package com.caro.bizkit.domain.card.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.card.dto.CardCreateResult;
import com.caro.bizkit.domain.card.dto.CardRequest;
import com.caro.bizkit.domain.card.dto.CardResponse;
import java.util.Map;
import com.caro.bizkit.domain.card.service.CardService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card", description = "명함 API")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "사용자 명함 목록 조회", description = "특정 사용자의 명함 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<CardResponse>>> getCardsByUserId(
            @RequestParam Integer userId
    ) {
        List<CardResponse> cards = cardService.getCardsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 명함 목록 조회 성공", cards));
    }

    @GetMapping("/{card_id}")
    @Operation(summary = "명함 단일 조회", description = "명함 ID로 명함을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<CardResponse>> getCardById(
            @PathVariable("card_id") Integer cardId
    ) {
        CardResponse card = cardService.getCardById(cardId);
        return ResponseEntity.ok(ApiResponse.success("명함 조회 성공", card));
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "UUID로 명함 조회", description = "UUID로 명함을 조회합니다. 인증 없이 접근 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<CardResponse>> getCardByUuid(
            @PathVariable("uuid") String uuid
    ) {
        CardResponse card = cardService.getCardByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success("명함 조회 성공", card));
    }

    @GetMapping("/me")
    @Operation(summary = "내 명함 조회", description = "인증된 사용자의 명함 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<CardResponse>>> getMyCards(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<CardResponse> cards = cardService.getMyCards(user);
        return ResponseEntity.ok(ApiResponse.success("내 명함 조회 성공", cards));
    }

    @GetMapping("/me/latest")
    @Operation(summary = "내 최신 명함 조회", description = "인증된 사용자의 가장 최근에 등록한 명함을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<CardResponse>> getMyLatestCard(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        CardResponse card = cardService.getMyLatestCard(user);
        return ResponseEntity.ok(ApiResponse.success("내 최신 명함 조회 성공", card));
    }

    @PostMapping("/me")
    @Operation(summary = "내 명함 생성", description = "인증된 사용자의 명함을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<CardResponse>> createMyCard(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CardRequest request
    ) {
        CardCreateResult result = cardService.createMyCard(user, request);
        String message = switch (result.resultType()) {
            case DUPLICATE -> "이미 동일한 명함이 존재합니다";
            case CLAIMED -> "기존의 종이 명함과 연결되었습니다";
            case CREATED -> "내 명함 생성 성공";
        };
        return ResponseEntity.ok(ApiResponse.success(message, result.card()));
    }

    @PatchMapping("/{card_id}")
    @Operation(summary = "내 명함 수정", description = "명함 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<CardResponse>> updateMyCard(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("card_id") Integer cardId,
            @RequestBody Map<String, Object> request
    ) {
        CardResponse card = cardService.updateMyCard(user, cardId, request);
        return ResponseEntity.ok(ApiResponse.success("내 명함 수정 성공", card));
    }

    @DeleteMapping("/{card_id}")
    @Operation(summary = "내 명함 삭제", description = "명함 정보를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMyCard(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("card_id") Integer cardId
    ) {
        cardService.deleteMyCard(user, cardId);
        return ResponseEntity.ok(ApiResponse.success("내 명함 삭제 성공", null));
    }
}
