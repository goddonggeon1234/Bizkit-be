package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.domain.ai.dto.AiCardJobMessage;
import com.caro.bizkit.domain.ai.entity.CardStyleTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCardGenerationService {

    private final RabbitTemplate rabbitTemplate;
    private final AiTaskCreator aiTaskCreator;

    public void generate(Integer userId, Integer cardId, CardStyleTag tag, String text) {
        AiCardJobMessage message = aiTaskCreator.createCardTask(userId, cardId, tag, text);

        try {
            rabbitTemplate.convertAndSend("ai.exchange", "card", message);
            log.info("User {} AI 명함 생성 잡 발행 backendTaskId={}", userId, message.backendTaskId());
        } catch (Exception e) {
            log.error("User {} AI 명함 생성 MQ 발행 실패 backendTaskId={}: {}", userId, message.backendTaskId(), e.getMessage());
        }
    }
}
