package com.caro.bizkit.domain.ai.rabbitmq.consumer;

import com.caro.bizkit.common.S3.dto.UploadCategory;
import com.caro.bizkit.common.S3.service.S3Service;
import com.caro.bizkit.domain.ai.entity.AiAnalysisStatus;
import com.caro.bizkit.domain.ai.entity.AiCardTask;
import com.caro.bizkit.domain.ai.rabbitmq.message.AiCardResultMessage;
import com.caro.bizkit.domain.ai.repository.AiCardTaskRepository;
import com.caro.bizkit.domain.ai.service.AiUsageService;
import com.caro.bizkit.domain.ai.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiCardResultConsumer {

    private final AiCardTaskRepository aiCardTaskRepository;
    private final AiUsageService aiUsageService;
    private final S3Service s3Service;
    private final SseEmitterService sseEmitterService;

    @RabbitListener(queues = "ai.card.result")
    @Transactional
    public void handle(AiCardResultMessage message) {
        log.info("[AI카드생성] 결과 수신 backendTaskId={}, userId={}, status={}",
                message.backendTaskId(), message.userId(), message.status());

        AiCardTask task = aiCardTaskRepository.findById(message.backendTaskId())
                .orElseThrow(() -> new IllegalStateException(
                        "AiCardTask를 찾을 수 없습니다. backendTaskId=" + message.backendTaskId()));

        if (task.getStatus() != AiAnalysisStatus.PENDING) {
            log.warn("[AI카드생성] 이미 처리된 메시지 skip backendTaskId={}", message.backendTaskId());
            return;
        }

        if ("failed".equals(message.status())) {
            log.error("[AI카드생성 실패] backendTaskId={}, aiTaskId={}, userId={}, error={}",
                    message.backendTaskId(), message.aiTaskId(), message.userId(), message.error());
            task.fail();
            sseEmitterService.sendFailed(message.userId(), "이미지 생성에 실패했습니다.");
            return;
        }

        String imageBase64 = message.data().imageBase64();
        byte[] imageBytes = decodeBase64Image(imageBase64);

        String key = s3Service.createObjectKey(UploadCategory.AI_CARD_TEMP, "card.png");
        s3Service.uploadBytes(key, imageBytes, "image/png");
        String publicUrl = s3Service.getPublicUrl(key);

        aiUsageService.decrement(message.userId());
        task.complete(publicUrl);

        sseEmitterService.sendCompleted(message.userId(), publicUrl);
        log.info("[AI카드생성] 완료 backendTaskId={}, userId={}", message.backendTaskId(), message.userId());
    }

    private byte[] decodeBase64Image(String base64) {
        String data = base64.contains(",") ? base64.split(",", 2)[1] : base64;
        return Base64.getDecoder().decode(data);
    }
}
