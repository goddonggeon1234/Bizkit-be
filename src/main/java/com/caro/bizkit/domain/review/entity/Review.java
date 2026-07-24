package com.caro.bizkit.domain.review.entity;


import com.caro.bizkit.common.entity.BaseTimeEntity;
import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review")
@SQLDelete(sql = "UPDATE review SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Review extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Column(length = 500)
    private String content;

    @Column(nullable = false)
    private Integer starScore; // TINYINT

    public static Review create(User reviewer, User reviewee, Integer starScore, String content) {
        Review review = new Review();
        review.reviewer = reviewer;
        review.reviewee = reviewee;
        review.starScore = starScore;
        review.content = content;
        return review;
    }

    public void update(Integer starScore, String content) {
        if (starScore != null) this.starScore = starScore;
        if (content != null) this.content = content;
    }
}
