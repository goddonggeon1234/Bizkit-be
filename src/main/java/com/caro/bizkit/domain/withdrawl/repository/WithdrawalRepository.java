package com.caro.bizkit.domain.withdrawl.repository;

import com.caro.bizkit.domain.withdrawl.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Integer> {
}
