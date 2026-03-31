package com.caro.bizkit.domain.auth.service;

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
import org.springframework.web.server.ResponseStatusException;
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
        if (!"kakao".equalsIgnoreCase(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider: " + provider);
        }
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.exchangeCodeForToken(code, redirectUri);
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to get access token");
        }


        KakaoUserResponse userResponse = kakaoOAuthClient.fetchUser(tokenResponse.accessToken());
        if (userResponse == null || userResponse.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch Kakao user profile");
        }
        KakaoUserResponse.KakaoAccount kakaoAccount = userResponse.kakaoAccount();
        KakaoUserResponse.KakaoProfile kakaoProfile = kakaoAccount.profile();

        String email = kakaoAccount.email();
        String nickname = kakaoProfile.nickname();


        Account account = oAuthRepository.findByProviderAndProviderId(provider, String.valueOf(userResponse.id()))
                .map(OAuth::getAccount)
                .orElseGet(() -> signUpAccount(provider, String.valueOf(userResponse.id()), email, nickname));

        account.updateLoggedAt(java.time.LocalDateTime.now());

        User user = userRepository.findByAccountAndDeletedAtIsNull(account)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No User Id in refresh token");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                String.valueOf(user.getId()),
                Map.of()
        );
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("토큰 재발행 성공: userId={}", userId);
        return new TokenPair(newAccessToken, newRefreshToken);
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
