package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.domain.ai.dto.AiHexJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiHexAnalysisService {

    private final RabbitTemplate rabbitTemplate;
    private final AiTaskCreator aiTaskCreator;

    public void analyze(Integer userId) {
        AiHexJobMessage message = aiTaskCreator.createHexTask(userId);
        if (message == null) return;

        try {
            rabbitTemplate.convertAndSend("ai.exchange", "hex", message);
            log.info("User {} AI 차트 분석 잡 발행 backendTaskId={}", userId, message.backendTaskId());
        } catch (Exception e) {
            log.error("User {} AI 차트 분석 MQ 발행 실패 backendTaskId={}: {}", userId, message.backendTaskId(), e.getMessage());
        }
    }
}
