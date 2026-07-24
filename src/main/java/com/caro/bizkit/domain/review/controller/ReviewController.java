package com.caro.bizkit.domain.review.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.review.dto.request.ReviewCreateRequest;
import com.caro.bizkit.domain.review.dto.response.ReviewDetailResponse;
import com.caro.bizkit.domain.review.dto.response.ReviewSummaryResponse;
import com.caro.bizkit.domain.review.dto.response.TagResponse;
import com.caro.bizkit.domain.review.service.ReviewService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "리뷰 API")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "남의 집계 리뷰 조회", description = "내 지갑에 해당 사용자의 명함이 있어야 조회 가능합니다.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getUserReviewSummary(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("리뷰 조회 성공", reviewService.getUserReviewSummary(user, userId)));
    }

    @Operation(summary = "내 집계 리뷰 조회", description = "내가 받은 리뷰의 집계 결과(리뷰 수, 평균 별점, 베이지안 점수, 상위 태그 3개)를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getMyReviewSummary(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.success("내 리뷰 조회 성공", reviewService.getMyReviewSummary(user)));
    }

    @Operation(summary = "태그 전체 조회", description = "리뷰 작성 시 선택 가능한 태그 목록을 반환합니다.")
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags() {
        return ResponseEntity.ok(ApiResponse.success("태그 조회 성공", reviewService.getTags()));
    }

    @Operation(summary = "내가 쓴 리뷰 단일 조회", description = "내가 특정 상대에게 작성한 리뷰를 조회합니다. 작성 이력이 없으면 data: null을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> getMyReview(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam Integer revieweeId
    ) {
        return ResponseEntity.ok(ApiResponse.success("내가 쓴 리뷰 조회 성공", reviewService.getMyReview(user, revieweeId)));
    }


    @Operation(summary = "리뷰 작성", description = "태그 1~3개, 별점 1~5, 선택적 코멘트로 리뷰를 작성합니다. 본인 작성 및 중복 작성 불가.")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("리뷰 작성 성공", reviewService.createReview(user, request)));
    }

    @Operation(summary = "리뷰 수정", description = "내가 쓴 리뷰를 부분 수정합니다. null 필드는 변경하지 않습니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> updateReview(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(ApiResponse.success("리뷰 수정 성공", reviewService.updateReview(user, body)));
    }

    @Operation(summary = "리뷰 삭제", description = "내가 특정 상대에게 쓴 리뷰를 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam Integer revieweeId
    ) {
        reviewService.deleteReview(user, revieweeId);
        return ResponseEntity.ok(ApiResponse.success("리뷰 삭제 성공", null));
    }

}
