package com.caro.bizkit.domain.withdrawl.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.withdrawl.dto.WithdrawalResponse;
import com.caro.bizkit.domain.withdrawl.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/withdrawals")
@RequiredArgsConstructor
@Tag(name = "Withdrawal", description = "탈퇴 사유 API")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @GetMapping
    @Operation(summary = "탈퇴 사유 목록 조회", description = "모든 탈퇴 사유 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<WithdrawalResponse>>> getAllWithdrawals() {
        List<WithdrawalResponse> withdrawals = withdrawalService.getAllWithdrawals();
        return ResponseEntity.ok(ApiResponse.success("탈퇴 사유 목록 조회 성공", withdrawals));
    }
}
