package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.domain.ai.dto.AiJobAnalyzeRequest;
import com.caro.bizkit.domain.ai.dto.AiJobJobMessage;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTask;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTaskType;
import com.caro.bizkit.domain.ai.repository.AiAnalysisTaskRepository;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.userdetail.activity.entity.Activity;
import com.caro.bizkit.domain.userdetail.activity.repository.ActivityRepository;
import com.caro.bizkit.domain.userdetail.project.entity.Project;
import com.caro.bizkit.domain.userdetail.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private final CardRepository cardRepository;
    private final ProjectRepository projectRepository;
    private final ActivityRepository activityRepository;
    private final AiAnalysisTaskRepository taskRepository;
    private final TransactionTemplate transactionTemplate;

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
        Integer[] taskDbId = new Integer[1];
        AiJobJobMessage[] messageRef = new AiJobJobMessage[1];

        transactionTemplate.executeWithoutResult(status -> {
            Card card = cardRepository.findById(cardId).orElse(null);
            if (card == null || card.getUser() == null) {
                log.warn("Card {} 조회 실패 또는 익명 명함, AI 분석 건너뜀", cardId);
                return;
            }
            Integer userId = card.getUser().getId();
            List<Project> projects = projectRepository.findAllByUserId(userId);
            List<Activity> activities = activityRepository.findAllByUserId(userId);

            AiAnalysisTask task = AiAnalysisTask.create(card.getUser(), AiAnalysisTaskType.JOB);
            taskRepository.save(task);
            taskDbId[0] = task.getId();
            messageRef[0] = buildJobMessage(task.getId(), userId, card, projects, activities);
        });

        if (taskDbId[0] == null) return;

        try {
            rabbitTemplate.convertAndSend("ai.exchange", "job", messageRef[0]);
            log.info("Card {} AI 직무 분석 잡 발행 backendTaskId={}", cardId, taskDbId[0]);
        } catch (Exception e) {
            log.error("Card {} AI 직무 분석 MQ 발행 실패 backendTaskId={}: {}", cardId, taskDbId[0], e.getMessage());
        }
    }

    private AiJobJobMessage buildJobMessage(Integer backendTaskId, Integer userId, Card card,
                                             List<Project> projects, List<Activity> activities) {
        List<AiJobAnalyzeRequest.ProjectDto> projectDtos = projects.stream()
                .map(this::toProjectDto)
                .toList();

        List<AiJobAnalyzeRequest.AwardDto> awardDtos = activities.stream()
                .map(this::toAwardDto)
                .toList();

        return new AiJobJobMessage(
                backendTaskId, userId, card.getId(),
                card.getName(), card.getCompany(), card.getDepartment(), card.getPosition(),
                projectDtos, awardDtos
        );
    }

    private AiJobAnalyzeRequest.ProjectDto toProjectDto(Project project) {
        Integer periodMonths = calculatePeriodMonths(project.getStartDate(), project.getEndDate());
        return new AiJobAnalyzeRequest.ProjectDto(
                project.getName(),
                project.getContent(),
                periodMonths
        );
    }

    private AiJobAnalyzeRequest.AwardDto toAwardDto(Activity activity) {
        Integer year = activity.getWinDate() != null ? activity.getWinDate().getYear() : null;
        return new AiJobAnalyzeRequest.AwardDto(
                activity.getName(),
                year
        );
    }

    private Integer calculatePeriodMonths(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return null;
        }
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return (int) ChronoUnit.MONTHS.between(startDate, end);
    }
}
