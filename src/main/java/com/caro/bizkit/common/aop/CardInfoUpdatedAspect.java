package com.caro.bizkit.common.aop;

import com.caro.bizkit.domain.ai.event.CardInfoUpdatedEvent;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class CardInfoUpdatedAspect {

    private final CardRepository cardRepository;
    private final ApplicationEventPublisher eventPublisher;

    @AfterReturning("@annotation(com.caro.bizkit.common.aop.CardInfoUpdated)")
    public void publish(JoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UserPrincipal principal) {
                cardRepository
                        .findTopByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(principal.id())
                        .ifPresent(card -> eventPublisher.publishEvent(
                                new CardInfoUpdatedEvent(card.getId(), "CARD", LocalDateTime.now())
                        ));
                return;
            }
        }
    }
}
