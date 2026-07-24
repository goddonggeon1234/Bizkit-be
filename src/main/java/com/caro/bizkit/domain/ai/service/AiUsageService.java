package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.ai.dto.AiUsageResponse;
import com.caro.bizkit.domain.ai.entity.AiUsage;
import com.caro.bizkit.domain.ai.repository.AiUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j

//todo
//테스트 해볼것
public class AiUsageService {

    private final AiUsageRepository aiUsageRepository;

    @Transactional(readOnly = true)
    public AiUsageResponse getUsage(Integer userId) {
        AiUsage usage = aiUsageRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용량 정보를 찾을 수 없습니다."));
        return new AiUsageResponse(usage.getWeeklyCount(), usage.getTotalCount());
    }

    @Transactional(readOnly = true)
    public void validateWeeklyCount(Integer userId) {
        AiUsage usage = aiUsageRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용량 정보를 찾을 수 없습니다."));
        if (usage.getWeeklyCount() <= 0) {
            throw new CustomException(HttpStatus.FORBIDDEN, "이번 주 사용 횟수를 모두 사용했습니다.");
        }
    }

    @Transactional
    public void decrement(Integer userId) {
        AiUsage usage = aiUsageRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용량 정보를 찾을 수 없습니다."));
        usage.decrementWeeklyCount();
    }

    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetWeeklyCount() {
        log.info("AI 사용량 주간 초기화 실행");
        aiUsageRepository.resetAllWeeklyCount();
    }
}
