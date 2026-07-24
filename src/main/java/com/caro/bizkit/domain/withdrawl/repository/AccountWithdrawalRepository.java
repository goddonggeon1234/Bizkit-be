package com.caro.bizkit.domain.withdrawl.repository;

import com.caro.bizkit.domain.withdrawl.entity.AccountWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountWithdrawalRepository extends JpaRepository<AccountWithdrawal, Integer> {
}
