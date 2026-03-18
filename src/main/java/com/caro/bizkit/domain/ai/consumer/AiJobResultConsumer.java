package com.caro.bizkit.domain.ai.consumer;

import com.caro.bizkit.domain.ai.dto.AiJobResultMessage;
import com.caro.bizkit.domain.ai.entity.AiAnalysisStatus;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTask;
import com.caro.bizkit.domain.ai.repository.AiAnalysisTaskRepository;
import com.caro.bizkit.domain.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJobResultConsumer {

    private final AiAnalysisTaskRepository aiAnalysisTaskRepository;
    private final CardRepository cardRepository;

    @RabbitListener(queues = "ai.job.result")
    @Transactional
    public void handle(AiJobResultMessage message) {
        log.info("[AI직무분석] 결과 수신 backendTaskId={}, cardId={}, status={}",
                message.backendTaskId(), message.cardId(), message.status());

        AiAnalysisTask task = aiAnalysisTaskRepository.findById(message.backendTaskId())
                .orElseThrow(() -> new IllegalStateException(
                        "AiAnalysisTask를 찾을 수 없습니다. backendTaskId=" + message.backendTaskId()));

        if (task.getStatus() != AiAnalysisStatus.PENDING) {
            log.warn("[AI직무분석] 이미 처리된 메시지 skip backendTaskId={}", message.backendTaskId());
            return;
        }

        if ("failed".equals(message.status())) {
            log.error("[AI직무분석 실패] backendTaskId={}, aiTaskId={}, cardId={}, error={}",
                    message.backendTaskId(), message.aiTaskId(), message.cardId(), message.error());
            task.fail();
            return;
        }

        cardRepository.findById(message.cardId())
                .ifPresent(card -> card.updateDescription(message.data().introduction()));
        task.complete();

        log.info("[AI직무분석] 완료 backendTaskId={}, cardId={}", message.backendTaskId(), message.cardId());
    }
}
