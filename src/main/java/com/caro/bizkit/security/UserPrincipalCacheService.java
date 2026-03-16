package com.caro.bizkit.security;

import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPrincipalCacheService {

    private final UserRepository userRepository;

    /**
     * 모든 인증 요청에서 발생하는 DB 조회를 캐싱.
     * null(탈퇴·미존재 유저)은 disableCachingNullValues 설정으로 캐시되지 않음.
     */
    @Cacheable(cacheNames = "principal", key = "#userId")
    public UserPrincipal findById(Integer userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(user -> new UserPrincipal(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getLinedNumber(),
                        user.getCompany(),
                        user.getDepartment(),
                        user.getPosition(),
                        user.getProfileImageKey(),
                        user.getDescription()
                ))
                .orElse(null);
    }

    @CacheEvict(cacheNames = "principal", key = "#userId")
    public void evict(Integer userId) {
    }
}
