package com.caro.bizkit.domain.user.repository;

import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.auth.entity.Account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByAccount(Account account);

    Optional<User> findByIdAndDeletedAtIsNull(Integer id);

    Optional<User> findByAccountAndDeletedAtIsNull(Account account);

    boolean existsByIdAndDeletedAtIsNull(Integer id);
}
