package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.ai.dto.AiCardJobMessage;
import com.caro.bizkit.domain.ai.entity.AiAnalysisStatus;
import com.caro.bizkit.domain.ai.entity.AiCardTask;
import com.caro.bizkit.domain.ai.entity.CardStyleTag;
import com.caro.bizkit.domain.ai.repository.AiCardTaskRepository;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCardGenerationService {

    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    private final AiCardTaskRepository aiCardTaskRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AiUsageService aiUsageService;

    public void generate(Integer userId, Integer cardId, CardStyleTag tag, String text) {
        Integer[] taskDbId = new Integer[1];
        AiCardJobMessage[] messageRef = new AiCardJobMessage[1];

        transactionTemplate.executeWithoutResult(status -> {
            aiUsageService.validateWeeklyCount(userId);

            boolean isActive = aiCardTaskRepository.existsByUser_IdAndStatusIn(
                    userId, List.of(AiAnalysisStatus.PENDING));
            if (isActive) {
                throw new CustomException(HttpStatus.CONFLICT, "이미 명함 이미지 생성이 진행 중입니다.");
            }

            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "명함을 찾을 수 없습니다."));
            if (card.getUser() == null || !card.getUser().getId().equals(userId)) {
                throw new CustomException(HttpStatus.FORBIDDEN, "본인의 명함만 요청할 수 있습니다.");
            }

            User user = userRepository.getReferenceById(userId);
            AiCardTask task = AiCardTask.create(user, card);
            aiCardTaskRepository.save(task);
            taskDbId[0] = task.getId();
            messageRef[0] = buildJobMessage(task.getId(), userId, cardId, card, tag, text);
        });

        if (taskDbId[0] == null) return;

        try {
            rabbitTemplate.convertAndSend("ai.exchange", "card", messageRef[0]);
            log.info("User {} AI 명함 생성 잡 발행 backendTaskId={}", userId, taskDbId[0]);
        } catch (Exception e) {
            log.error("User {} AI 명함 생성 MQ 발행 실패 backendTaskId={}: {}", userId, taskDbId[0], e.getMessage());
        }
    }

    private AiCardJobMessage buildJobMessage(Integer backendTaskId, Integer userId, Integer cardId,
                                              Card card, CardStyleTag tag, String text) {
        return new AiCardJobMessage(
                backendTaskId, userId, cardId, tag.name(), text,
                card.getName(), card.getCompany(), card.getDepartment(), card.getPosition(),
                card.getPhoneNumber(), card.getEmail()
        );
    }
}
