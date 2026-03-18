package com.caro.bizkit.domain.ai.entity;

import com.caro.bizkit.common.entity.BaseTimeEntity;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ai_card_task",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_card_task_active_token", columnNames = {"active_token"})
        }
)
public class AiCardTask extends BaseTimeEntity implements AiTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAnalysisStatus status;

    @Column(name = "ai_task_id")
    private String aiTaskId;

    @Column(name = "result_image_url", length = 500)
    private String resultImageUrl;

    @Column(name = "active_token")
    private Integer activeToken;

    public static AiCardTask create(User user, Card card) {
        AiCardTask task = new AiCardTask();
        task.user = user;
        task.card = card;
        task.status = AiAnalysisStatus.PENDING;
        task.activeToken = user.getId();
        return task;
    }

    public void assignAiTaskId(String aiTaskId) {
        this.aiTaskId = aiTaskId;
    }

    public void complete(String resultImageUrl) {
        this.status = AiAnalysisStatus.COMPLETED;
        this.resultImageUrl = resultImageUrl;
        this.activeToken = null;
    }

    public void fail() {
        this.status = AiAnalysisStatus.FAILED;
        this.activeToken = null;
    }

    @Override
    public Integer getUserId() {
        return user.getId();
    }
}
