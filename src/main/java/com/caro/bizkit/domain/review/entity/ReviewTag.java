package com.caro.bizkit.domain.review.entity;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_tag")
public class ReviewTag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static ReviewTag create(Review review, Tag tag) {
        ReviewTag reviewTag = new ReviewTag();
        reviewTag.review = review;
        reviewTag.tag = tag;
        return reviewTag;
    }
}