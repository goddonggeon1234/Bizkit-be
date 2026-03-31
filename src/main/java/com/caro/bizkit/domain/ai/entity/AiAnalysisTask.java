package com.caro.bizkit.domain.ai.entity;

import com.caro.bizkit.common.entity.BaseTimeEntity;
import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_analysis_task")
public class AiAnalysisTask extends BaseTimeEntity implements AiTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAnalysisStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAnalysisTaskType taskType;

    @Column(name = "ai_task_id")
    private String aiTaskId;

    public static AiAnalysisTask create(User user, AiAnalysisTaskType taskType) {
        AiAnalysisTask task = new AiAnalysisTask();
        task.user = user;
        task.status = AiAnalysisStatus.PENDING;
        task.taskType = taskType;
        return task;
    }

    public void complete() {
        this.status = AiAnalysisStatus.COMPLETED;
    }

    public void fail() {
        this.status = AiAnalysisStatus.FAILED;
    }

    public void assignAiTaskId(String aiTaskId) {
        this.aiTaskId = aiTaskId;
    }

    @Override
    public Integer getUserId() {
        return user.getId();
    }
}
