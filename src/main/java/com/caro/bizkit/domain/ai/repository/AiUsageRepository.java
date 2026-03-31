package com.caro.bizkit.domain.ai.repository;

import com.caro.bizkit.domain.ai.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AiUsageRepository extends JpaRepository<AiUsage, Integer> {

    void deleteByUserId(Integer userId);

    Optional<AiUsage> findByUserId(Integer userId);

    @Modifying
    @Query("UPDATE AiUsage a SET a.weeklyCount = 3")
    void resetAllWeeklyCount();
}
