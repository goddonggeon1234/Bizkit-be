package com.caro.bizkit.domain.ai.event;

import com.caro.bizkit.domain.ai.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardInfoUpdateListener {

    private final AiAnalysisService aiAnalysisService;

    @EventListener
    public void handleCardInfoUpdated(CardInfoUpdatedEvent event) {
        log.info("Received CardInfoUpdatedEvent: cardId={}, type={}", event.resourceId(), event.updateType());
        aiAnalysisService.addToBatch(event.resourceId());
    }
}
