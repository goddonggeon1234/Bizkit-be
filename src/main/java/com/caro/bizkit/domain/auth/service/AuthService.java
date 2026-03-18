package com.caro.bizkit.domain.auth.service;

import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.auth.dto.KakaoTokenResponse;
import com.caro.bizkit.domain.auth.dto.KakaoUserResponse;
import com.caro.bizkit.domain.auth.dto.TokenPair;
import com.caro.bizkit.domain.auth.entity.Account;
import com.caro.bizkit.domain.auth.entity.OAuth;
import com.caro.bizkit.domain.auth.repository.AccountRepository;
import com.caro.bizkit.domain.auth.repository.OAuthRepository;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.ai.entity.AiUsage;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.ai.repository.AiUsageRepository;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.security.JwtTokenProvider;
import com.caro.bizkit.security.RefreshTokenService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final AccountRepository accountRepository;
    private final OAuthRepository oAuthRepository;
    private final UserRepository userRepository;
    private final AiUsageRepository aiUsageRepository;
    private final CardRepository cardRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;


    @Transactional
    public TokenPair login(String provider, String code, String redirectUri) {

        validateProvider(provider);
        KakaoUserResponse userResponse = fetchKakaoUser(code, redirectUri);

        KakaoUserResponse.KakaoAccount kakaoAccount = userResponse.kakaoAccount();
        KakaoUserResponse.KakaoProfile kakaoProfile = kakaoAccount.profile();
        String email = kakaoAccount.email();
        String nickname = kakaoProfile.nickname();

        Account account = oAuthRepository.findByProviderAndProviderId(provider, String.valueOf(userResponse.id()))
                .map(OAuth::getAccount)
                .orElseGet(() -> signUpAccount(provider, String.valueOf(userResponse.id()), email, nickname));

        account.updateLoggedAt(java.time.LocalDateTime.now());

        User user = userRepository.findByAccountAndDeletedAtIsNull(account)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String accessToken = jwtTokenProvider.generateAccessToken(
                String.valueOf(user.getId()),
                Map.of()
        );
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("logged in with {}", account.getLoginEmail());
        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional()
    public TokenPair refresh(String refreshToken) {
        Integer userId = refreshTokenService.validateAndGetUserId(refreshToken);
        if (userId == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "리프레시 토큰에 사용자 정보가 없습니다.");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                String.valueOf(user.getId()),
                Map.of()
        );
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("토큰 재발행 성공: userId={}", userId);
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    private void validateProvider(String provider) {
        if (!"kakao".equalsIgnoreCase(provider)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 제공자입니다.");
        }
    }

    private KakaoUserResponse fetchKakaoUser(String code, String redirectUri) {
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.exchangeCodeForToken(code, redirectUri);
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new CustomException(HttpStatus.BAD_GATEWAY, "카카오 액세스 토큰을 가져오지 못했습니다.");
        }
        KakaoUserResponse userResponse = kakaoOAuthClient.fetchUser(tokenResponse.accessToken());
        if (userResponse == null || userResponse.id() == null) {
            throw new CustomException(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보를 가져오지 못했습니다.");
        }
        return userResponse;
    }

    public void logout(Integer userId) {
        refreshTokenService.deleteRefreshToken(userId);
        log.info("User logged out: {}", userId);
    }

    @Transactional
    public Account signUpAccount(String provider, String providerId, String loginEmail, String nickname) {
        Account account = Account.create(loginEmail);
        Account savedAccount = accountRepository.save(account);

        OAuth oauth = OAuth.create(savedAccount, provider, providerId);
        oAuthRepository.save(oauth);

        User user = User.create(savedAccount, nickname, loginEmail);
        AiUsage aiUsage = AiUsage.create(user);
        userRepository.save(user);
        aiUsageRepository.save(aiUsage);

        cardRepository.findAllByUserIsNullAndDeletedAtIsNullAndNameAndEmail(nickname, loginEmail)
                .forEach(card -> card.setUser(user));

        log.info("Account created: {}", loginEmail);

        return savedAccount;
    }
}
