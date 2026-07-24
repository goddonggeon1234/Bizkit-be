package com.caro.bizkit.domain.withdrawl.service;

import com.caro.bizkit.domain.withdrawl.dto.WithdrawalResponse;
import com.caro.bizkit.domain.withdrawl.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;

    @Transactional(readOnly = true)
    public List<WithdrawalResponse> getAllWithdrawals() {
        return withdrawalRepository.findAll().stream()
                .map(WithdrawalResponse::from)
                .toList();
    }
}
