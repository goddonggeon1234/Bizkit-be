package com.caro.bizkit.domain.review.repository;

import com.caro.bizkit.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    boolean existsByReviewer_IdAndReviewee_Id(Integer reviewerId, Integer revieweeId);

    java.util.Optional<Review> findByReviewer_IdAndReviewee_Id(Integer reviewerId, Integer revieweeId);

    @Query("SELECT COUNT(r), COALESCE(SUM(r.starScore), 0) FROM Review r WHERE r.reviewee.id = :revieweeId")
    Object[] findAggregateByRevieweeId(@Param("revieweeId") Integer revieweeId);

    @Query("SELECT r.content FROM Review r WHERE r.reviewee.id = :revieweeId AND r.content IS NOT NULL")
    java.util.List<String> findTextReviewsByRevieweeId(@Param("revieweeId") Integer revieweeId);
}
