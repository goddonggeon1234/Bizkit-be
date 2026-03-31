package com.caro.bizkit.domain.userdetail.activity.service;

import com.caro.bizkit.common.aop.CardInfoUpdated;
import com.caro.bizkit.common.security.CardCollectionValidator;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.userdetail.activity.dto.ActivityRequest;
import com.caro.bizkit.domain.userdetail.activity.dto.ActivityResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;
import com.caro.bizkit.domain.userdetail.activity.entity.Activity;
import com.caro.bizkit.domain.userdetail.activity.repository.ActivityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final CardCollectionValidator cardCollectionValidator;

    @Transactional(readOnly = true)
    public List<ActivityResponse> getMyActivities(UserPrincipal principal) {
        return activityRepository.findAllByUserIdAndDeletedAtIsNull(principal.id()).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivitiesByUserId(UserPrincipal principal, Integer userId) {
        cardCollectionValidator.validateAccess(principal.id(), userId);
        return activityRepository.findAllByUserIdAndDeletedAtIsNull(userId).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @CardInfoUpdated
    @Transactional
    public ActivityResponse createMyActivity(UserPrincipal principal, ActivityRequest request) {
        User user = userRepository.getReferenceById(principal.id());
        Activity activity = Activity.create(
                user,
                request.name(),
                request.grade(),
                request.organization(),
                request.content(),
                request.win_date()
        );
        return ActivityResponse.from(activityRepository.save(activity));
    }

    @CardInfoUpdated
    @Transactional
    @PreAuthorize("@activitySecurity.isOwner(#activityId, authentication)")
    public ActivityResponse updateMyActivity(
            UserPrincipal principal,
            Integer activityId,
            Map<String, Object> request
    ) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (request == null) {
            return ActivityResponse.from(activity);
        }

        applyUpdates(activity, request);
        return ActivityResponse.from(activity);
    }

    @CardInfoUpdated
    @Transactional
    @PreAuthorize("@activitySecurity.isOwner(#activityId, authentication)")
    public void deleteMyActivity(UserPrincipal principal, Integer activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        activityRepository.delete(activity);
    }

    private void applyUpdates(Activity activity, Map<String, Object> request) {
        applyIfPresent(request, "name", activity::updateName);
        applyIfPresent(request, "grade", activity::updateGrade);
        applyIfPresent(request, "organization", activity::updateOrganization);
        applyIfPresent(request, "content", activity::updateContent);
        applyDateIfPresent(request, "win_date", activity::updateWinDate);
    }

    private void applyIfPresent(Map<String, Object> request, String key, Consumer<String> updater) {
        if (request.containsKey(key)) {
            updater.accept((String) request.get(key));
        }
    }

    private void applyDateIfPresent(Map<String, Object> request, String key, Consumer<LocalDate> updater) {
        if (request.containsKey(key)) {
            Object value = request.get(key);
            if (value instanceof LocalDate) {
                updater.accept((LocalDate) value);
            } else if (value instanceof String) {
                updater.accept(LocalDate.parse((String) value));
            }
        }
    }
}
