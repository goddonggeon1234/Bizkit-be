package com.caro.bizkit.domain.ai.rabbitmq.consumer;

import com.caro.bizkit.domain.ai.entity.AiTask;
import com.caro.bizkit.domain.ai.rabbitmq.message.AiDlqMessage;
import com.caro.bizkit.domain.ai.repository.AiAnalysisTaskRepository;
import com.caro.bizkit.domain.ai.repository.AiCardTaskRepository;
import com.caro.bizkit.domain.ai.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiResultDlqConsumer {

    private final AiCardTaskRepository aiCardTaskRepository;
    private final AiAnalysisTaskRepository aiAnalysisTaskRepository;
    private final SseEmitterService sseEmitterService;

    @RabbitListener(queues = "ai.result.dlq")
    @Transactional
    public void handle(
            @Payload AiDlqMessage message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) {
        log.error("[AI result DLQ] 재시도 초과 backendTaskId={}, userId={}, routingKey={}",
                message.backendTaskId(), message.userId(), routingKey);

        AiTask task = findTask(message.backendTaskId(), routingKey);
        if (task == null) {
            log.error("[AI result DLQ] task를 찾을 수 없음 backendTaskId={}", message.backendTaskId());
            return;
        }

        task.fail();

        if ("card.result".equals(routingKey)) {
            sseEmitterService.sendFailed(message.userId(), "AI 이미지 생성에 실패했습니다.");
        }
    }

    private AiTask findTask(Integer backendTaskId, String routingKey) {
        if ("card.result".equals(routingKey)) {
            return aiCardTaskRepository.findById(backendTaskId).orElse(null);
        }
        return aiAnalysisTaskRepository.findById(backendTaskId).orElse(null);
    }
}
