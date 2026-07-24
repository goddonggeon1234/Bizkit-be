package com.caro.bizkit.domain.ai.event;

import com.caro.bizkit.domain.ai.service.AiHexAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HexAnalysisTriggerListener {

    private final AiHexAnalysisService aiHexAnalysisService;

    @Async
    @EventListener
    public void onHexAnalysisTrigger(HexAnalysisTriggerEvent event) {
        log.info("User {} 차트 분석 트리거 수신", event.userId());
        aiHexAnalysisService.analyze(event.userId());
    }
}
