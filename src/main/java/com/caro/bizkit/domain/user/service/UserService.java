package com.caro.bizkit.domain.user.service;

import com.caro.bizkit.common.S3.service.S3Service;
import com.caro.bizkit.common.security.CardCollectionValidator;

import com.caro.bizkit.domain.auth.entity.Account;
import com.caro.bizkit.domain.auth.entity.OAuth;
import com.caro.bizkit.domain.auth.repository.OAuthRepository;
import com.caro.bizkit.domain.auth.service.KakaoOAuthClient;
import com.caro.bizkit.domain.auth.service.KakaoOAuthProperties;
import com.caro.bizkit.domain.card.repository.UserCardRepository;
import com.caro.bizkit.domain.ai.repository.AiUsageRepository;
import com.caro.bizkit.domain.userdetail.skill.repository.UserSkillRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.dto.UserResponse;
import java.util.Map;
import java.util.function.Consumer;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final OAuthRepository oAuthRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;

    private final UserCardRepository userCardRepository;
    private final CardCollectionValidator cardCollectionValidator;
    private final UserSkillRepository userSkillRepository;
    private final AiUsageRepository aiUsageRepository;

    public UserResponse getMyStatus(UserPrincipal user) {
        String profileImageUrl = null;
        if (user != null && StringUtils.hasText(user.profile_image_key())) {
            profileImageUrl = s3Service.getPublicUrl(user.profile_image_key());
        }
        return UserResponse.fromPrincipal(user, profileImageUrl);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UserPrincipal principal, Integer userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        cardCollectionValidator.validateAccess(principal.id(), userId);

        return toResponse(user);
    }

    @CacheEvict(cacheNames = "principal", key = "#principal.id")
    @Transactional
    public UserResponse updateMyStatus(UserPrincipal principal, Map<String, Object> request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (request == null) {
            return toResponse(user);
        }

        String previousKey = user.getProfileImageKey();
        applyUpdates(user, request);

        if (previousKey != null
                && request.containsKey("profile_image_key")
                && !previousKey.equals(request.get("profile_image_key"))) {
            s3Service.deleteObject(previousKey);
        }

        return toResponse(user);
    }

    private void applyUpdates(User user, Map<String, Object> request) {
        applyIfPresent(request, "name", user::updateName);
        applyIfPresent(request, "profile_image_key", user::updateProfileImageKey);
    }

    private void applyIfPresent(Map<String, Object> request, String key, Consumer<String> updater) {
        if (request.containsKey(key)) {
            updater.accept((String) request.get(key));
        }
    }

    @CacheEvict(cacheNames = "principal", key = "#principal.id")
    @Transactional
    public void withdraw(UserPrincipal principal) {

        User user = userRepository.findByIdAndDeletedAtIsNull(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
        Account account = user.getAccount();

        // 관련 테이블 삭제
        userCardRepository.deleteAllByUserId(principal.id());
        userSkillRepository.deleteAllByUserId(principal.id());
        aiUsageRepository.deleteByUserId(principal.id());

        oAuthRepository.findByAccount(account).ifPresent(oauth -> {
            unlinkFromKakaoIfNeeded(oauth);
            oAuthRepository.delete(oauth);
        });

        account.markDeleted();
        user.markDeleted();
    }

    private void unlinkFromKakaoIfNeeded(OAuth oauth) {
        if (!"kakao".equalsIgnoreCase(oauth.getProvider())) {
            log.warn("Unsupported OAuth provider for unlink: {}", oauth.getProvider());
            return;
        }
        kakaoOAuthClient.unlinkUserByAdminKey(
                kakaoOAuthProperties.getAdminKey(),
                oauth.getProviderId()
        );
    }

    private UserResponse toResponse(User user) {
        String profileImageUrl = null;
        if (StringUtils.hasText(user.getProfileImageKey())) {
            profileImageUrl = s3Service.getPublicUrl(user.getProfileImageKey());
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                profileImageUrl
        );
    }
}
