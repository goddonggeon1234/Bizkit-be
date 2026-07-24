package com.caro.bizkit.domain.userdetail.activity.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.userdetail.activity.dto.ActivityRequest;
import com.caro.bizkit.domain.userdetail.activity.dto.ActivityResponse;
import java.util.Map;
import com.caro.bizkit.domain.userdetail.activity.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activity", description = "활동 정보 조회 API")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/me")
    @Operation(summary = "내 활동 조회", description = "인증된 사용자의 활동 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getMyActivities(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<ActivityResponse> activities = activityService.getMyActivities(user);
        return ResponseEntity.ok(ApiResponse.success("내 활동 조회 성공", activities));
    }

    @GetMapping
    @Operation(summary = "사용자 활동 조회", description = "지정된 사용자 활동 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivitiesByUserId(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("userId") Integer userId
    ) {
        List<ActivityResponse> activities = activityService.getActivitiesByUserId(principal, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 활동 조회 성공", activities));
    }

    @PostMapping("/me")
    @Operation(summary = "내 활동 생성", description = "인증된 사용자의 활동을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<ActivityResponse>> createMyActivity(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ActivityRequest request
    ) {
        ActivityResponse activity = activityService.createMyActivity(user, request);
        return ResponseEntity.ok(ApiResponse.success("내 활동 생성 성공", activity));
    }

    @PatchMapping("/{activity_id}")
    @Operation(summary = "내 활동 수정", description = "활동 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<ActivityResponse>> updateMyActivity(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("activity_id") Integer activityId,
            @RequestBody Map<String, Object> request
    ) {
        ActivityResponse activity = activityService.updateMyActivity(user, activityId, request);
        return ResponseEntity.ok(ApiResponse.success("내 활동 수정 성공", activity));
    }

    @DeleteMapping("/{activity_id}")
    @Operation(summary = "내 활동 삭제", description = "활동 정보를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMyActivity(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("activity_id") Integer activityId
    ) {
        activityService.deleteMyActivity(user, activityId);
        return ResponseEntity.ok(ApiResponse.success("내 활동 삭제 성공", null));
    }
}
