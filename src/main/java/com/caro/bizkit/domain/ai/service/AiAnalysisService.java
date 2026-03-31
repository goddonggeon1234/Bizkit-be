package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.domain.ai.rabbitmq.message.AiJobJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private static final long DEBOUNCE_SECONDS = 10;

    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final RabbitTemplate rabbitTemplate;
    private final AiTaskCreator aiTaskCreator;

    public void addToBatch(Integer cardId) {
        pendingTasks.compute(cardId, (id, existing) -> {
            if (existing != null && !existing.isDone()) {
                existing.cancel(false);
                log.info("Card {} 디바운스 리셋 (10초 재시작)", id);
            }
            return scheduler.schedule(() -> {
                pendingTasks.remove(id);
                processCard(id);
            }, DEBOUNCE_SECONDS, TimeUnit.SECONDS);
        });
        log.info("Card {} AI 분석 예약 ({}초 후)", cardId, DEBOUNCE_SECONDS);
    }

    private void processCard(Integer cardId) {
        AiJobJobMessage message = aiTaskCreator.createJobTask(cardId);
        if (message == null) return;

        try {
            rabbitTemplate.convertAndSend("ai.exchange", "job", message);
            log.info("Card {} AI 직무 분석 잡 발행 backendTaskId={}", cardId, message.backendTaskId());
        } catch (Exception e) {
            log.error("Card {} AI 직무 분석 MQ 발행 실패 backendTaskId={}: {}", cardId, message.backendTaskId(), e.getMessage());
        }
    }
}
