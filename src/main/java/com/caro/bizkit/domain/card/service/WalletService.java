package com.caro.bizkit.domain.card.service;

import com.caro.bizkit.domain.card.dto.CardCollectRequest;
import com.caro.bizkit.domain.card.dto.CardOcrRequest;
import com.caro.bizkit.domain.card.dto.CardResponse;
import com.caro.bizkit.domain.card.dto.CollectedCardsResult;
import com.caro.bizkit.domain.card.dto.WalletResponse;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.entity.UserCard;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.card.repository.UserCardRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import com.caro.bizkit.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;

    @Transactional()
    public WalletResponse collectCard(UserPrincipal principal, CardCollectRequest request) {
        Card card = cardRepository.findByUuid(request.uuid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        if (card.getUser() != null && card.getUser().getId().equals(principal.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot collect your own card");
        }
        if (userCardRepository.existsByUserIdAndCardId(principal.id(), card.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card already collected");
        }



        User user = userRepository.getReferenceById(principal.id());
        UserCard userCard = UserCard.create(user, card);
        userCardRepository.save(userCard);
        return WalletResponse.from(card);
    }
    @Transactional(readOnly = true)
    public CollectedCardsResult getCollectedCards(
            UserPrincipal principal,
            Integer size,
            Integer cursorId,
            String keyword
    ) {
        int limit = normalizeSize(size);
        List<UserCard> userCards = findUserCards(principal.id(), cursorId, keyword, limit + 1);
        boolean hasNext = userCards.size() > limit;
        if (hasNext) {
            userCards = userCards.subList(0, limit);
        }
        Integer nextCursorId = userCards.isEmpty() ? null : userCards.getLast().getId();
        List<WalletResponse> cards = userCards.stream()
                .map(UserCard::getCard)
                .map(WalletResponse::from)
                .toList();
        return new CollectedCardsResult(cards, nextCursorId, hasNext);
    }

    @Transactional
    public CardResponse createAnonymousCard(UserPrincipal principal, CardOcrRequest request) {
        // ① 본인 명함 여부 확인
        String baseName = cardRepository.findTopByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(principal.id())
                .map(Card::getName)
                .orElse(principal.name());
        if (request.name().equals(baseName) && request.email().equals(principal.email())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "본인 명함은 등록할 수 없습니다");
        }

        // ② 중복 명함 조회
        Optional<Card> existing = StringUtils.hasText(request.position())
                ? cardRepository.findFirstByDeletedAtIsNullAndNameAndEmailAndCompanyAndPositionOrderByCreatedAtDesc(
                        request.name(), request.email(), request.company(), request.position())
                : cardRepository.findFirstByDeletedAtIsNullAndNameAndEmailAndCompanyOrderByCreatedAtDesc(
                        request.name(), request.email(), request.company());

        User user = userRepository.getReferenceById(principal.id());

        if (existing.isPresent()) {
            Card card = existing.get();

            // ③ 카드 주인 확인
            if (card.getUser() != null && card.getUser().getId().equals(principal.id())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "본인 명함은 등록할 수 없습니다");
            }
            if (userCardRepository.existsByUserIdAndCardId(principal.id(), card.getId())) {
                throw new CustomException(HttpStatus.CONFLICT, "이미 수집한 명함입니다");
            }
            userCardRepository.save(UserCard.create(user, card));
            return CardResponse.from(card);
        }

        // ④ 익명 Card 생성 후 수집
        Card anonymousCard = cardRepository.save(Card.create(
                null,
                Card.newUuid(),
                request.name(),
                request.email(),
                request.phone_number(),
                request.lined_number(),
                request.company(),
                request.position(),
                request.department(),
                LocalDate.now(),
                null,
                null
        ));
        userCardRepository.save(UserCard.create(user, anonymousCard));
        return CardResponse.from(anonymousCard);
    }

    @Transactional
    public void deleteCollectedCard(UserPrincipal principal, Integer cardId) {
        UserCard userCard = userCardRepository.findByUserIdAndCardId(principal.id(), cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collected card not found"));
        userCardRepository.delete(userCard);
    }

    private List<UserCard> findUserCards(Integer userId, Integer cursorId, String keyword, int limit) {
        if (StringUtils.hasText(keyword)) {
            String escaped = keyword.replace("\\", "\\\\")
                                    .replace("%", "\\%")
                                    .replace("_", "\\_");
            List<Integer> ids = cursorId == null
                    ? userCardRepository.searchCollectedCardIds(userId, escaped, PageRequest.of(0, limit))
                    : userCardRepository.searchCollectedCardIdsWithCursor(userId, cursorId, escaped, PageRequest.of(0, limit));
            if (ids.isEmpty()) {
                return List.of();
            }
            return userCardRepository.findAllByIdInWithFetch(ids);
        }
        if (cursorId == null) {
            return userCardRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, limit));
        }
        return userCardRepository.findByUserIdAndIdLessThanOrderByIdDesc(
                userId,
                cursorId,
                PageRequest.of(0, limit)
        );
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return size;
    }


}
