package com.caro.bizkit.domain.userdetail.activity.entity;

import com.caro.bizkit.common.entity.BaseTimeEntity;
import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "activity")
@SQLDelete(sql = "UPDATE activity SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Activity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 50)
    private String grade;

    @Column(length = 50, nullable = false)
    private String organization;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDate winDate;

    public static Activity create(
            User user,
            String name,
            String grade,
            String organization,
            String content,
            LocalDate winDate
    ) {
        Activity activity = new Activity();
        activity.user = user;
        activity.name = name;
        activity.grade = grade;
        activity.organization = organization;
        activity.content = content;
        activity.winDate = winDate;
        return activity;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateGrade(String grade) {
        this.grade = grade;
    }

    public void updateOrganization(String organization) {
        this.organization = organization;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateWinDate(LocalDate winDate) {
        this.winDate = winDate;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
