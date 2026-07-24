package com.caro.bizkit.domain.auth.repository;

import com.caro.bizkit.domain.auth.entity.OAuth;
import com.caro.bizkit.domain.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthRepository extends JpaRepository<OAuth, Integer> {
    Optional<OAuth> findByProviderAndProviderId(String provider, String providerId);
    Optional<OAuth> findByAccount(Account account);
}
