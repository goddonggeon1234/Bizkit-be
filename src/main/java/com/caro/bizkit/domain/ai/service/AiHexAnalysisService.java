package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.domain.ai.dto.AiHexAnalyzeRequest;
import com.caro.bizkit.domain.ai.dto.AiHexJobMessage;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTask;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTaskType;
import com.caro.bizkit.domain.ai.repository.AiAnalysisTaskRepository;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.review.repository.ReviewRepository;
import com.caro.bizkit.domain.review.repository.ReviewTagRepository;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.userdetail.activity.entity.Activity;
import com.caro.bizkit.domain.userdetail.activity.repository.ActivityRepository;
import com.caro.bizkit.domain.userdetail.link.entity.Link;
import com.caro.bizkit.domain.userdetail.link.repository.LinkRepository;
import com.caro.bizkit.domain.userdetail.project.entity.Project;
import com.caro.bizkit.domain.userdetail.project.repository.ProjectRepository;
import com.caro.bizkit.domain.userdetail.skill.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiHexAnalysisService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String GITHUB_DOMAIN = "github.com";

    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProjectRepository projectRepository;
    private final ActivityRepository activityRepository;
    private final LinkRepository linkRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final AiAnalysisTaskRepository taskRepository;

    public void analyze(Integer userId) {
        Integer[] taskDbId = new Integer[1];
        AiHexJobMessage[] messageRef = new AiHexJobMessage[1];

        transactionTemplate.executeWithoutResult(status -> {
            Optional<Link> githubLink = linkRepository.findFirstByUserIdAndLinkContaining(userId, GITHUB_DOMAIN);
            if (githubLink.isEmpty()) {
                log.info("User {} GitHub 링크 없음, 차트 분석 건너뜀", userId);
                return;
            }

            User user = userRepository.getReferenceById(userId);
            AiAnalysisTask task = AiAnalysisTask.create(user, AiAnalysisTaskType.HEX);
            taskRepository.save(task);
            taskDbId[0] = task.getId();
            messageRef[0] = buildJobMessage(task.getId(), userId, githubLink.get().getLink());
        });

        if (taskDbId[0] == null) return;

        try {
            rabbitTemplate.convertAndSend("ai.exchange", "hex", messageRef[0]);
            log.info("User {} AI 차트 분석 잡 발행 backendTaskId={}", userId, taskDbId[0]);
        } catch (Exception e) {
            log.error("User {} AI 차트 분석 MQ 발행 실패 backendTaskId={}: {}", userId, taskDbId[0], e.getMessage());
        }
    }

    private AiHexJobMessage buildJobMessage(Integer backendTaskId, Integer userId, String githubUrl) {
        String githubUsername = extractGithubUsername(githubUrl);

        List<Card> cards = cardRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(userId);
        List<String> skills = userSkillRepository.findAllByUserId(userId).stream()
                .map(us -> us.getSkill().getName())
                .limit(10)
                .toList();
        List<Project> projects = projectRepository.findAllByUserId(userId);
        List<Activity> activities = activityRepository.findAllByUserId(userId);

        long totalReviews = reviewRepository.findAggregateByRevieweeId(userId)[0] instanceof Number n ? n.longValue() : 0L;
        List<String> textReviews = reviewRepository.findTextReviewsByRevieweeId(userId);
        AiHexAnalyzeRequest.Reviews reviews = buildReviews(userId, totalReviews, textReviews);

        return new AiHexJobMessage(
                backendTaskId,
                userId,
                githubUsername,
                new AiHexAnalyzeRequest.Capabilities(
                        cards.stream().map(this::toCareer).toList(),
                        skills,
                        projects.stream().map(this::toProject).toList(),
                        activities.stream().map(this::toAchievement).toList()
                ),
                reviews
        );
    }

    private AiHexAnalyzeRequest.Reviews buildReviews(Integer userId, long totalReviews, List<String> textReviews) {
        if (totalReviews == 0) return null;

        Map<String, Long> tagCounts = reviewTagRepository.findTagCountsByRevieweeId(userId).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));

        AiHexAnalyzeRequest.BadgeReviews badge = new AiHexAnalyzeRequest.BadgeReviews(
                ratio(tagCounts, "협업을 잘한다.", totalReviews),
                ratio(tagCounts, "말을 잘한다.", totalReviews),
                ratio(tagCounts, "기술역량이 뛰어나다.", totalReviews),
                ratio(tagCounts, "문서화를 잘한다.", totalReviews),
                ratio(tagCounts, "일정을 안지킨다.", totalReviews),
                ratio(tagCounts, "다음에 같이 일하고 싶지 않다.", totalReviews)
        );

        return new AiHexAnalyzeRequest.Reviews(textReviews.isEmpty() ? null : textReviews, badge);
    }

    private double ratio(Map<String, Long> tagCounts, String keyword, long total) {
        return tagCounts.getOrDefault(keyword, 0L) / (double) total;
    }

    private AiHexAnalyzeRequest.Career toCareer(Card card) {
        return new AiHexAnalyzeRequest.Career(
                card.getCompany(),
                card.getDepartment(),
                card.getPosition(),
                card.getStartDate() != null ? card.getStartDate().format(YEAR_MONTH) : null,
                card.getEndDate() != null ? card.getEndDate().format(YEAR_MONTH) : null
        );
    }

    private AiHexAnalyzeRequest.Project toProject(Project project) {
        return new AiHexAnalyzeRequest.Project(
                project.getName(),
                project.getContent(),
                project.getStartDate() != null ? project.getStartDate().format(YEAR_MONTH) : null,
                project.getEndDate() != null ? project.getEndDate().format(YEAR_MONTH) : null
        );
    }

    private AiHexAnalyzeRequest.Achievement toAchievement(Activity activity) {
        return new AiHexAnalyzeRequest.Achievement(
                activity.getName(),
                activity.getGrade(),
                activity.getOrganization(),
                activity.getContent(),
                activity.getWinDate() != null ? activity.getWinDate().toString() : null
        );
    }

    private String extractGithubUsername(String url) {
        try {
            String[] parts = url.replaceAll("https?://", "").split("/");
            return parts.length > 1 ? parts[1] : parts[0].replace("github.com", "").trim();
        } catch (Exception e) {
            return "";
        }
    }
}
