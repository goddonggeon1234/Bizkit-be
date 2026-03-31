package com.caro.bizkit.domain.review.dto.response;

import com.caro.bizkit.domain.review.entity.Review;
import com.caro.bizkit.domain.review.entity.ReviewTag;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReviewDetailResponse(
        @JsonProperty("review_id") Integer reviewId,
        Integer score,
        String comment,
        List<TagResponse> tags
) {
    public static ReviewDetailResponse of(Review review, List<ReviewTag> reviewTags) {
        return new ReviewDetailResponse(
                review.getId(),
                review.getStarScore(),
                review.getContent(),
                reviewTags.stream().map(rt -> TagResponse.from(rt.getTag())).toList()
        );
    }
}
