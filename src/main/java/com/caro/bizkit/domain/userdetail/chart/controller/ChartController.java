package com.caro.bizkit.domain.userdetail.chart.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.userdetail.chart.dto.ChartItemResponse;
import com.caro.bizkit.domain.userdetail.chart.service.ChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
@Tag(name = "Chart", description = "6각 차트 API")
public class ChartController {

    private final ChartService chartService;

    @Operation(summary = "내 차트 조회", description = "내 6각 레이더 차트 분석 결과를 반환합니다. 분석 결과가 없으면 빈 배열을 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ChartItemResponse>>> getMyChart(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.success("육각 차트 조회 성공", chartService.getMyChart(user)));
    }

    @Operation(summary = "남의 차트 조회", description = "내 지갑에 해당 사용자의 명함이 있어야 조회 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChartItemResponse>>> getUserChart(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam Integer userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("육각 차트 조회 성공", chartService.getUserChart(user, userId)));
    }
}
