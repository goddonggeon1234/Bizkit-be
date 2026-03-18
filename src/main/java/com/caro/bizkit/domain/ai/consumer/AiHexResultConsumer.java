package com.caro.bizkit.domain.ai.consumer;

import com.caro.bizkit.domain.ai.dto.AiHexResultMessage;
import com.caro.bizkit.domain.ai.entity.AiAnalysisStatus;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTask;
import com.caro.bizkit.domain.ai.repository.AiAnalysisTaskRepository;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.userdetail.chart.entity.ChartData;
import com.caro.bizkit.domain.userdetail.chart.repository.ChartDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiHexResultConsumer {

    private final AiAnalysisTaskRepository aiAnalysisTaskRepository;
    private final ChartDataRepository chartDataRepository;
    private final UserRepository userRepository;

    @RabbitListener(queues = "ai.hex.result")
    @Transactional
    public void handle(AiHexResultMessage message) {
        log.info("[AI차트분석] 결과 수신 backendTaskId={}, userId={}, status={}",
                message.backendTaskId(), message.userId(), message.status());

        AiAnalysisTask task = aiAnalysisTaskRepository.findById(message.backendTaskId())
                .orElseThrow(() -> new IllegalStateException(
                        "AiAnalysisTask를 찾을 수 없습니다. backendTaskId=" + message.backendTaskId()));

        if (task.getStatus() != AiAnalysisStatus.PENDING) {
            log.warn("[AI차트분석] 이미 처리된 메시지 skip backendTaskId={}", message.backendTaskId());
            return;
        }

        if ("failed".equals(message.status())) {
            log.error("[AI차트분석 실패] backendTaskId={}, aiTaskId={}, userId={}, error={}",
                    message.backendTaskId(), message.aiTaskId(), message.userId(), message.error());
            task.fail();
            return;
        }

        saveChartData(message.userId(), message.data());
        task.complete();

        log.info("[AI차트분석] 완료 backendTaskId={}, userId={}", message.backendTaskId(), message.userId());
    }

    private void saveChartData(Integer userId, AiHexResultMessage.Data data) {
        chartDataRepository.deleteAllByUserId(userId);
        User user = userRepository.getReferenceById(userId);
        AiHexResultMessage.RadarChart radar = data.radarChart();
        AiHexResultMessage.AnalysisSummary summary = data.analysisSummary();

        List.of(
                ChartData.create(user, "collaboration", radar.collaboration(), summary.collaboration()),
                ChartData.create(user, "communication", radar.communication(), summary.communication()),
                ChartData.create(user, "technical", radar.technical(), summary.technical()),
                ChartData.create(user, "documentation", radar.documentation(), summary.documentation()),
                ChartData.create(user, "reliability", radar.reliability(), summary.reliability()),
                ChartData.create(user, "preference", radar.preference(), summary.preference())
        ).forEach(chartDataRepository::save);
    }
}
