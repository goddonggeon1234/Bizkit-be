package com.caro.bizkit.domain.review.repository;

import com.caro.bizkit.domain.review.entity.ReviewTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewTagRepository extends JpaRepository<ReviewTag, Integer> {

    @EntityGraph(attributePaths = "tag")
    List<ReviewTag> findAllByReview_Id(Integer reviewId);

    @Modifying
    @Query("DELETE FROM ReviewTag rt WHERE rt.review.id = :reviewId")
    void deleteAllByReviewId(@Param("reviewId") Integer reviewId);

    @Query("""
            SELECT rt.tag.id, rt.tag.keyword, COUNT(rt)
            FROM ReviewTag rt
            WHERE rt.review.reviewee.id = :revieweeId
            GROUP BY rt.tag.id, rt.tag.keyword
            ORDER BY COUNT(rt) DESC
            """)
    List<Object[]> findTopTagsByRevieweeId(@Param("revieweeId") Integer revieweeId, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT rt.tag.keyword, COUNT(rt)
            FROM ReviewTag rt
            WHERE rt.review.reviewee.id = :revieweeId
            GROUP BY rt.tag.keyword
            """)
    List<Object[]> findTagCountsByRevieweeId(@Param("revieweeId") Integer revieweeId);
}
