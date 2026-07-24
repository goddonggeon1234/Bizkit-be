package com.caro.bizkit.domain.card.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.common.ApiResponse.Pagination;
import com.caro.bizkit.domain.card.dto.CardCollectRequest;
import com.caro.bizkit.domain.card.dto.CardOcrRequest;
import com.caro.bizkit.domain.card.dto.CardResponse;
import com.caro.bizkit.domain.card.dto.CollectedCardsResult;
import com.caro.bizkit.domain.card.dto.WalletResponse;
import com.caro.bizkit.domain.card.service.WalletService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Validated
@Tag(name = "Wallet", description = "명함 지갑 API")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/paper-cards")
    @Operation(summary = "OCR 명함 등록", description = "OCR로 인식한 명함 데이터를 등록하고 수집합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409")
    })
    public ResponseEntity<ApiResponse<CardResponse>> registerPaperCard(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CardOcrRequest request
    ) {
        CardResponse card = walletService.createAnonymousCard(user, request);
        return ResponseEntity.ok(ApiResponse.success("OCR 명함 등록 성공", card));
    }

    @PostMapping()
    @Operation(summary = "상대방 명함 수집", description = "QR로 받은 uuid를 통해 명함을 수집합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<WalletResponse>> collectCard(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CardCollectRequest request
    ) {
        WalletResponse card = walletService.collectCard(user, request);
        return ResponseEntity.ok(ApiResponse.success("명함 수집 성공", card));
    }

    @GetMapping()
    @Operation(summary = "수집한 명함 조회", description = "수집한 명함 목록을 커서 기반으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "message": "수집 명함 조회 성공",
                              "data": [
                                {
                                  "id": 1,
                                  "user_id": 10,
                                  "uuid": "550e8400-e29b-41d4-a716-446655440000",
                                  "name": "홍길동",
                                  "email": "hong@example.com",
                                  "phone_number": "010-1234-5678",
                                  "lined_number": "02-1234-5678",
                                  "company": "카로",
                                  "position": "백엔드 개발자",
                                  "department": "개발팀",
                                  "ai_image_key": "images/ai/abc.png"
                                }
                              ],
                              "pagination": {
                                "cursorId": 1,
                                "has_next": true
                              }
                            }
                            """)))
    })
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getCollectedCards(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "20") @Max(value = 30, message = "size는 최대 30까지 허용됩니다") Integer size,
            @RequestParam(required = false) Integer cursorId,
            @RequestParam(required = false) @Size(max = 100, message = "검색어는 100자 이하로 입력해주세요") String keyword
    ) {
        CollectedCardsResult result = walletService.getCollectedCards(user, size, cursorId, keyword);
        Pagination pagination = new Pagination(result.cursorId(), result.hasNext());
        return ResponseEntity.ok(ApiResponse.successWithPagination("수집 명함 조회 성공", result.data(), pagination));
    }

    @DeleteMapping("/{card_id}")
    @Operation(summary = "수집한 명함 삭제", description = "수집한 명함을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCollectedCard(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("card_id") Integer cardId
    ) {
        walletService.deleteCollectedCard(user, cardId);
        return ResponseEntity.ok(ApiResponse.success("수집 명함 삭제 성공", null));
    }
}
