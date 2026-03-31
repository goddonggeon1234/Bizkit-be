package com.caro.bizkit.domain.withdrawl.dto;

import com.caro.bizkit.domain.withdrawl.entity.Withdrawal;

public record WithdrawalResponse(
        Integer id,
        String content
) {
    public static WithdrawalResponse from(Withdrawal withdrawal) {
        return new WithdrawalResponse(
                withdrawal.getId(),
                withdrawal.getContent()
        );
    }
}
