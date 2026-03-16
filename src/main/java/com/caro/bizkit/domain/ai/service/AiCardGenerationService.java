package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.common.S3.dto.UploadCategory;
import com.caro.bizkit.common.S3.service.S3Service;
import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.ai.client.AiAnalysisClient;
import com.caro.bizkit.domain.ai.config.AiClientProperties;
import com.caro.bizkit.domain.ai.dto.AiCardGenerateRequest;
import com.caro.bizkit.domain.ai.dto.AiCardGenerateResponse;
import com.caro.bizkit.domain.ai.dto.AiJobSubmitResponse;
import com.caro.bizkit.domain.ai.dto.AiTaskStatusResponse;
import com.caro.bizkit.domain.ai.entity.AiAnalysisStatus;
import com.caro.bizkit.domain.ai.entity.AiCardTask;
import com.caro.bizkit.domain.ai.repository.AiCardTaskRepository;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCardGenerationService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final AiAnalysisClient aiClient;
    private final AiClientProperties properties;
    private final TransactionTemplate transactionTemplate;

    private final AiCardTaskRepository aiCardTaskRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AiUsageService aiUsageService;
    private final S3Service s3Service;
    private final SseEmitterService sseEmitterService;

    public void generate(Integer userId, Integer cardId, String tag, String text) {
        Integer[] taskDbId = new Integer[1];
        AiCardGenerateRequest[] requestRef = new AiCardGenerateRequest[1];

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
            requestRef[0] = buildRequest(userId, card, tag, text);
        });

        if (taskDbId[0] == null) return;

        AiJobSubmitResponse submitResponse;
        try {
            submitResponse = aiClient.submitCardGeneration(requestRef[0]);
        } catch (Exception e) {
            log.error("User {} AI 명함 생성 요청 실패: {}", userId, e.getMessage());
            failTask(userId, taskDbId[0], "AI 서버 요청에 실패했습니다.");
            return;
        }

        String aiTaskId = submitResponse.taskId();
        log.info("User {} AI 명함 생성 요청 완료, aiTaskId={}", userId, aiTaskId);

        transactionTemplate.executeWithoutResult(status ->
                aiCardTaskRepository.findById(taskDbId[0]).ifPresent(task -> task.assignAiTaskId(aiTaskId)));

        long deadline = System.currentTimeMillis() + properties.getCard().getTimeoutSeconds() * 1000L;
        scheduler.schedule(() -> poll(userId, taskDbId[0], aiTaskId, deadline),
                properties.getCard().getPollIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void poll(Integer userId, Integer taskDbId, String aiTaskId, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            log.warn("User {} AI 명함 생성 시간 초과", userId);
            failTask(userId, taskDbId, "생성 시간이 초과되었습니다.");
            return;
        }

        try {
            AiTaskStatusResponse statusResponse = aiClient.getTaskStatus(aiTaskId);

            if ("completed".equals(statusResponse.status())) {
                Optional<AiCardGenerateResponse> result = aiClient.getCardTaskResult(aiTaskId);
                result.ifPresent(res -> handleCompleted(userId, taskDbId, res));
                if (result.isEmpty()) schedulePoll(userId, taskDbId, aiTaskId, deadline);
            } else if ("failed".equals(statusResponse.status())) {
                log.warn("User {} AI 명함 생성 실패", userId);
                failTask(userId, taskDbId, "이미지 생성에 실패했습니다.");
            } else {
                sseEmitterService.sendProgress(userId, statusResponse.status(), statusResponse.progress());
                schedulePoll(userId, taskDbId, aiTaskId, deadline);
            }
        } catch (Exception e) {
            log.error("User {} polling 오류: {}", userId, e.getMessage());
            failTask(userId, taskDbId, "이미지 생성 중 오류가 발생했습니다.");
        }
    }

    private void failTask(Integer userId, Integer taskDbId, String message) {
        transactionTemplate.executeWithoutResult(status ->
                aiCardTaskRepository.findById(taskDbId).ifPresent(AiCardTask::fail));
        sseEmitterService.sendFailed(userId, message);
    }

    private void schedulePoll(Integer userId, Integer taskDbId, String aiTaskId, long deadline) {
        scheduler.schedule(() -> poll(userId, taskDbId, aiTaskId, deadline),
                properties.getCard().getPollIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void handleCompleted(Integer userId, Integer taskDbId, AiCardGenerateResponse res) {
        String tempKey;
        try {
            byte[] imageBytes = decodeBase64Image(res.data().imageDataUrl());
            tempKey = s3Service.createObjectKey(UploadCategory.AI_CARD_TEMP, "card.png");
            s3Service.uploadBytes(tempKey, imageBytes, "image/png");
        } catch (Exception e) {
            log.error("User {} S3 업로드 실패: {}", userId, e.getMessage());
            failTask(userId, taskDbId, "이미지 저장에 실패했습니다.");
            return;
        }

        String publicUrl = s3Service.getPublicUrl(tempKey);

        transactionTemplate.executeWithoutResult(status -> {
            aiUsageService.decrement(userId);
            aiCardTaskRepository.findById(taskDbId).ifPresent(task -> task.complete(publicUrl));
        });

        sseEmitterService.sendCompleted(userId, publicUrl);
        log.info("User {} AI 명함 생성 완료", userId);
    }

    private byte[] decodeBase64Image(String dataUrl) {
        String base64 = dataUrl.contains(",") ? dataUrl.split(",", 2)[1] : dataUrl;
        return Base64.getDecoder().decode(base64);
    }

    private AiCardGenerateRequest buildRequest(Integer userId, Card card, String tag, String text) {
        return new AiCardGenerateRequest(
                userId,
                new AiCardGenerateRequest.CardInfo(
                        card.getName(),
                        card.getCompany(),
                        card.getDepartment(),
                        card.getPosition(),
                        card.getPhoneNumber(),
                        card.getEmail(),
                        null
                ),
                new AiCardGenerateRequest.Style(tag, text)
        );
    }
}
