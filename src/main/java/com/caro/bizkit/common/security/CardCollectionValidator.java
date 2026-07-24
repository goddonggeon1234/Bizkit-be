package com.caro.bizkit.common.security;

import com.caro.bizkit.domain.card.repository.UserCardRepository;
import com.caro.bizkit.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CardCollectionValidator {

    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;

    /**
     * 요청자가 대상 사용자의 카드를 수집했는지 검증합니다.
     * 대상 사용자가 존재하지 않거나 삭제된 경우 NOT_FOUND 예외를 던집니다.
     * 자기 자신이면 통과, 수집한 카드의 주인이 아니면 NOT_FOUND 예외를 던집니다.
     */
    public void validateAccess(Integer requesterId, Integer targetUserId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");
        }
        if (requesterId.equals(targetUserId)) {
            return;
        }
        boolean hasCollectedCard = userCardRepository.existsCollectedCardByOwner(requesterId, targetUserId);
        if (!hasCollectedCard) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "정보를 찾을 수 없습니다");
        }
    }
}
