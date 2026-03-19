package com.caro.bizkit.domain.ai.service;

import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.ai.dto.AiCardJobMessage;
import com.caro.bizkit.domain.ai.dto.AiHexAnalyzeRequest;
import com.caro.bizkit.domain.ai.dto.AiHexJobMessage;
import com.caro.bizkit.domain.ai.dto.AiJobAnalyzeRequest;
import com.caro.bizkit.domain.ai.dto.AiJobJobMessage;
import com.caro.bizkit.domain.ai.entity.AiAnalysisStatus;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTask;
import com.caro.bizkit.domain.ai.entity.AiAnalysisTaskType;
import com.caro.bizkit.domain.ai.entity.AiCardTask;
import com.caro.bizkit.domain.ai.entity.CardStyleTag;
import com.caro.bizkit.domain.ai.repository.AiAnalysisTaskRepository;
import com.caro.bizkit.domain.ai.repository.AiCardTaskRepository;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.review.repository.ReviewRepository;
import com.caro.bizkit.domain.review.repository.ReviewTagRepository;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.userdetail.activity.entity.Activity;
import com.caro.bizkit.domain.userdetail.activity.repository.ActivityRepository;
import com.caro.bizkit.domain.userdetail.link.repository.LinkRepository;
import com.caro.bizkit.domain.userdetail.project.entity.Project;
import com.caro.bizkit.domain.userdetail.project.repository.ProjectRepository;
import com.caro.bizkit.domain.userdetail.skill.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiTaskCreator {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String GITHUB_DOMAIN = "github.com";

    private final AiCardTaskRepository aiCardTaskRepository;
    private final AiAnalysisTaskRepository aiAnalysisTaskRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProjectRepository projectRepository;
    private final ActivityRepository activityRepository;
    private final LinkRepository linkRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final AiUsageService aiUsageService;

    @Transactional(readOnly = true)
    public AiCardJobMessage createCardTask(Integer userId, Integer cardId, CardStyleTag tag, String text) {
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

        return new AiCardJobMessage(
                task.getId(), userId, cardId, tag.name(), text,
                card.getName(), card.getCompany(), card.getDepartment(), card.getPosition(),
                card.getPhoneNumber(), card.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public AiJobJobMessage createJobTask(Integer cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card == null || card.getUser() == null) {
            log.warn("Card {} 조회 실패 또는 익명 명함, AI 분석 건너뜀", cardId);
            return null;
        }

        Integer userId = card.getUser().getId();
        List<Project> projects = projectRepository.findAllByUserId(userId);
        List<Activity> activities = activityRepository.findAllByUserId(userId);

        AiAnalysisTask task = AiAnalysisTask.create(card.getUser(), AiAnalysisTaskType.JOB);
        aiAnalysisTaskRepository.save(task);

        return new AiJobJobMessage(
                task.getId(), userId, cardId,
                card.getName(), card.getCompany(), card.getDepartment(), card.getPosition(),
                projects.stream().map(this::toProjectDto).toList(),
                activities.stream().map(this::toAwardDto).toList()
        );
    }

    @Transactional(readOnly = true)
    public AiHexJobMessage createHexTask(Integer userId) {
        return linkRepository.findFirstByUserIdAndLinkContaining(userId, GITHUB_DOMAIN)
                .map(link -> buildHexJobMessage(userId, link.getLink()))
                .orElseGet(() -> {
                    log.info("User {} GitHub 링크 없음, 차트 분석 건너뜀", userId);
                    return null;
                });
    }

    private AiHexJobMessage buildHexJobMessage(Integer userId, String githubUrl) {
        String githubUsername = extractGithubUsername(githubUrl);
        List<Card> cards = cardRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(userId);
        List<String> skills = userSkillRepository.findAllByUserId(userId).stream()
                .map(us -> us.getSkill().getName()).limit(10).toList();
        List<Project> projects = projectRepository.findAllByUserId(userId);
        List<Activity> activities = activityRepository.findAllByUserId(userId);

        long totalReviews = reviewRepository.findAggregateByRevieweeId(userId)[0] instanceof Number n ? n.longValue() : 0L;
        List<String> textReviews = reviewRepository.findTextReviewsByRevieweeId(userId);
        AiHexAnalyzeRequest.Reviews reviews = buildReviews(userId, totalReviews, textReviews);

        User user = userRepository.getReferenceById(userId);
        AiAnalysisTask task = AiAnalysisTask.create(user, AiAnalysisTaskType.HEX);
        aiAnalysisTaskRepository.save(task);

        return new AiHexJobMessage(
                task.getId(), userId, githubUsername,
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

    private AiJobAnalyzeRequest.ProjectDto toProjectDto(Project project) {
        Integer periodMonths = null;
        if (project.getStartDate() != null) {
            LocalDate end = project.getEndDate() != null ? project.getEndDate() : LocalDate.now();
            periodMonths = (int) ChronoUnit.MONTHS.between(project.getStartDate(), end);
        }
        return new AiJobAnalyzeRequest.ProjectDto(project.getName(), project.getContent(), periodMonths);
    }

    private AiJobAnalyzeRequest.AwardDto toAwardDto(Activity activity) {
        Integer year = activity.getWinDate() != null ? activity.getWinDate().getYear() : null;
        return new AiJobAnalyzeRequest.AwardDto(activity.getName(), year);
    }

    private AiHexAnalyzeRequest.Career toCareer(Card card) {
        return new AiHexAnalyzeRequest.Career(
                card.getCompany(), card.getDepartment(), card.getPosition(),
                card.getStartDate() != null ? card.getStartDate().format(YEAR_MONTH) : null,
                card.getEndDate() != null ? card.getEndDate().format(YEAR_MONTH) : null
        );
    }

    private AiHexAnalyzeRequest.Project toProject(Project project) {
        return new AiHexAnalyzeRequest.Project(
                project.getName(), project.getContent(),
                project.getStartDate() != null ? project.getStartDate().format(YEAR_MONTH) : null,
                project.getEndDate() != null ? project.getEndDate().format(YEAR_MONTH) : null
        );
    }

    private AiHexAnalyzeRequest.Achievement toAchievement(Activity activity) {
        return new AiHexAnalyzeRequest.Achievement(
                activity.getName(), activity.getGrade(), activity.getOrganization(),
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
