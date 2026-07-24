package com.caro.bizkit.domain.review.service;

import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.ai.event.HexAnalysisTriggerEvent;
import com.caro.bizkit.domain.review.dto.request.ReviewCreateRequest;
import com.caro.bizkit.domain.review.dto.response.ReviewDetailResponse;
import com.caro.bizkit.domain.review.dto.response.ReviewSummaryResponse;
import com.caro.bizkit.domain.review.dto.response.TagCountResponse;
import com.caro.bizkit.domain.review.dto.response.TagResponse;
import com.caro.bizkit.domain.review.entity.Review;
import com.caro.bizkit.domain.review.entity.ReviewTag;
import com.caro.bizkit.domain.review.entity.Tag;
import com.caro.bizkit.domain.review.repository.ReviewRepository;
import com.caro.bizkit.domain.review.repository.ReviewTagRepository;
import com.caro.bizkit.domain.card.repository.UserCardRepository;
import com.caro.bizkit.domain.review.repository.TagRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final TagRepository tagRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final UserRepository userRepository;
    private final UserCardRepository userCardRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Cacheable(cacheNames = "tags", cacheManager = "caffeineCacheManager")
    @Transactional(readOnly = true)
    public List<TagResponse> getTags() {
        return tagRepository.findAll().stream()
                .map(TagResponse::from)
                .toList();
    }

    @Transactional
    public ReviewDetailResponse createReview(UserPrincipal principal, ReviewCreateRequest request) {
        if (principal.id().equals(request.revieweeId())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "본인에게 리뷰를 작성할 수 없습니다.");
        }
        if (reviewRepository.existsByReviewer_IdAndReviewee_Id(principal.id(), request.revieweeId())) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다.");
        }

        User reviewer = userRepository.getReferenceById(principal.id());
        User reviewee = userRepository.getReferenceById(request.revieweeId());
        Review review = reviewRepository.save(Review.create(reviewer, reviewee, request.score(), request.comment()));

        request.tagIdList().forEach(tagId -> {
            Tag tag = tagRepository.getReferenceById(tagId);
            reviewTagRepository.save(ReviewTag.create(review, tag));
        });

        List<ReviewTag> reviewTags = reviewTagRepository.findAllByReview_Id(review.getId());

        long reviewCount = ((Number) reviewRepository.findAggregateByRevieweeId(request.revieweeId())[0]).longValue();
        if (reviewCount % 10 == 0) {
            eventPublisher.publishEvent(new HexAnalysisTriggerEvent(request.revieweeId()));
        }

        return ReviewDetailResponse.of(review, reviewTags);
    }

    @Transactional
    public ReviewDetailResponse updateReview(UserPrincipal principal, Map<String, Object> body) {
        Integer revieweeId = (Integer) body.get("reviewee_id");
        Review review = reviewRepository.findByReviewer_IdAndReviewee_Id(principal.id(), revieweeId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        if (!review.getReviewer().getId().equals(principal.id())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인의 리뷰만 수정할 수 있습니다.");
        }
        applyReviewUpdates(review, body);
        return ReviewDetailResponse.of(review, reviewTagRepository.findAllByReview_Id(review.getId()));
    }

    private void applyReviewUpdates(Review review, Map<String, Object> body) {
        if (body.containsKey("score")) review.update((Integer) body.get("score"), null);
        if (body.containsKey("comment")) review.update(null, (String) body.get("comment"));
        if (body.containsKey("tag_id_list")) {
            List<?> rawList = (List<?>) body.get("tag_id_list");
            if (rawList.size() < 1 || rawList.size() > 3) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "태그는 1~3개 선택해야 합니다.");
            }
            reviewTagRepository.deleteAllByReviewId(review.getId());
            rawList.stream()
                    .map(id -> (Integer) id)
                    .forEach(tagId -> reviewTagRepository.save(ReviewTag.create(review, tagRepository.getReferenceById(tagId))));
        }
    }

    @Transactional
    public void deleteReview(UserPrincipal principal, Integer revieweeId) {
        Review review = reviewRepository.findByReviewer_IdAndReviewee_Id(principal.id(), revieweeId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        if (!review.getReviewer().getId().equals(principal.id())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인의 리뷰만 삭제할 수 있습니다.");
        }
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public ReviewDetailResponse getMyReview(UserPrincipal principal, Integer revieweeId) {
        return reviewRepository.findByReviewer_IdAndReviewee_Id(principal.id(), revieweeId)
                .map(review -> ReviewDetailResponse.of(review, reviewTagRepository.findAllByReview_Id(review.getId())))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getMyReviewSummary(UserPrincipal principal) {
        return buildSummary(principal.id());
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getUserReviewSummary(UserPrincipal principal, Integer userId) {
        if (!userCardRepository.existsCollectedCardByOwner(principal.id(), userId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "해당 사용자의 명함을 보유하고 있지 않습니다.");
        }
        return buildSummary(userId);
    }

    private ReviewSummaryResponse buildSummary(Integer revieweeId) {
        Object[] agg = reviewRepository.findAggregateByRevieweeId(revieweeId);
        int reviewCount = ((Number) agg[0]).intValue();
        int starScoreSum = ((Number) agg[1]).intValue();

        double averageScore = reviewCount == 0 ? 0.0 : Math.round((double) starScoreSum / reviewCount * 10.0) / 10.0;
        double calculatedScore = Math.round(calculateScore(reviewCount, starScoreSum) * 10.0) / 10.0;

        List<TagCountResponse> topTags = reviewTagRepository
                .findTopTagsByRevieweeId(revieweeId, PageRequest.of(0, 3))
                .stream()
                .map(row -> new TagCountResponse((Integer) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();

        return new ReviewSummaryResponse(reviewCount, averageScore, calculatedScore, topTags);
    }

    private double calculateScore(int reviewCount, int starScoreSum) {
        if (reviewCount == 0) return 30.0;
        double n = reviewCount;
        double sum = starScoreSum;
        double bayesAvg = (60 + sum) / (20 + n);
        double baseScore = 30 + 15 * (bayesAvg - 3);
        double countBonus = 4 * Math.log(1 + n) + 0.03 * n;
        return Math.clamp(baseScore + countBonus, 0.0, 100.0);
    }
}
