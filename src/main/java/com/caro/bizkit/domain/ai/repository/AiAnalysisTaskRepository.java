package com.caro.bizkit.domain.ai.repository;

import com.caro.bizkit.domain.ai.entity.AiAnalysisTask;
import com.caro.bizkit.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiAnalysisTaskRepository extends JpaRepository<AiAnalysisTask, Integer> {

    List<AiAnalysisTask> findByUserOrderByCreatedAtDesc(User user);

    Optional<AiAnalysisTask> findTopByUserOrderByCreatedAtDesc(User user);
}
