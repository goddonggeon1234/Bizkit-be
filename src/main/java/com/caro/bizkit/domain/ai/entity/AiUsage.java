package com.caro.bizkit.domain.ai.entity;

import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ai_usage")
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer weeklyCount = 3;

    @Column(nullable = false)
    private Integer totalCount = 0;

    @LastModifiedDate
    private LocalDateTime lastUsedAt;

    public static AiUsage create(User user) {
        AiUsage aiUsage = new AiUsage();
        aiUsage.user = user;
        return aiUsage;
    }

    public void decrementWeeklyCount() {
        this.weeklyCount -= 1;
        this.totalCount += 1;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
