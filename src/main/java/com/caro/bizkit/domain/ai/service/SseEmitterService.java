package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.domain.ai.entity.AiAnalysisStatus;
import com.caro.bizkit.domain.ai.entity.AiCardTask;
import com.caro.bizkit.domain.ai.repository.AiCardTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 220_000L;
    private static final String SSE_CHANNEL = "sse:notifications";

    private final ConcurrentHashMap<Integer, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final AiCardTaskRepository aiCardTaskRepository;
    private final ObjectMapper objectMapper;

    public SseEmitter connect(Integer userId) {
        Optional<AiCardTask> latestTask = aiCardTaskRepository.findTopByUser_IdOrderByCreatedAtDesc(userId);
        if (latestTask.isPresent()) {
            AiCardTask task = latestTask.get();
            if (task.getStatus() == AiAnalysisStatus.FAILED && isWithinOneSecond(task.getUpdatedAt())) {
                return immediateEmitter("failed", Map.of("event", "failed", "error", "이미지 생성에 실패했습니다."));
            }
            if (task.getStatus() == AiAnalysisStatus.COMPLETED && isWithinOneSecond(task.getUpdatedAt())) {
                return immediateEmitter("completed", Map.of("event", "completed", "image_url", task.getResultImageUrl()));
            }
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean cleaned = new AtomicBoolean(false);

        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("failed").data(Map.of("error", "timeout")));
            } catch (Exception ignored) {}
        });

        SseEmitter existing = emitterMap.put(userId, emitter);
        if (existing != null) {
            existing.complete();
        }

        emitter.onCompletion(() -> {
            if (cleaned.compareAndSet(false, true)) {
                emitterMap.remove(userId, emitter);
            }
        });

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("status", "connected")));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void sendCompleted(Integer userId, String imageUrl) {
        publish(userId, Map.of("event", "completed", "image_url", imageUrl));
    }

    public void sendFailed(Integer userId, String error) {
        publish(userId, Map.of("event", "failed", "error", error));
    }

    public void handleSseMessage(String message, String channel) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            Integer userId = Integer.valueOf((String) data.get("userId"));
            String event = (String) data.get("event");

            SseEmitter emitter = emitterMap.get(userId);
            if (emitter == null) {
                log.warn("[SSE] emitter 없음 userId={} — 메시지 무시", userId);
                return;
            }

            Map<String, Object> payload = new HashMap<>(data);
            payload.remove("userId");

            log.info("[SSE] 이벤트 전송 시작 userId={}, event={}", userId, event);
            emitter.send(SseEmitter.event().name(event).data(payload));
            log.info("[SSE] 이벤트 전송 완료 userId={}, event={}", userId, event);

            if ("completed".equals(event) || "failed".equals(event)) {
                emitter.complete();
                log.info("[SSE] emitter complete userId={}", userId);
            }
        } catch (Exception e) {
            log.error("[SSE] 메시지 처리 실패: {} ({})", e.getMessage(), e.getClass().getSimpleName());
        }
    }

    private void publish(Integer userId, Map<String, String> data) {
        try {
            Map<String, String> payload = new HashMap<>(data);
            payload.put("userId", String.valueOf(userId));
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(SSE_CHANNEL, json);
        } catch (JsonProcessingException e) {
            log.error("SSE 이벤트 직렬화 실패: {}", e.getMessage());
        }
    }

    private boolean isWithinOneSecond(LocalDateTime time) {
        return time != null && ChronoUnit.MILLIS.between(time, LocalDateTime.now()) <= 1000;
    }

    private SseEmitter immediateEmitter(String event, Map<String, String> data) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
