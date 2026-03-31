package com.caro.bizkit.domain.auth.repository;

import com.caro.bizkit.domain.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Integer> {
}
