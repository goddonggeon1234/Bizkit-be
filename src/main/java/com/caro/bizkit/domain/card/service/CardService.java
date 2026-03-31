package com.caro.bizkit.domain.card.service;

import com.caro.bizkit.domain.ai.event.CardInfoUpdatedEvent;
import com.caro.bizkit.domain.card.dto.CardCreateResult;
import com.caro.bizkit.domain.card.dto.CardCreateResult.ResultType;
import com.caro.bizkit.domain.card.dto.CardRequest;
import com.caro.bizkit.domain.card.dto.CardResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.util.StringUtils;
import java.util.Map;
import java.util.function.Consumer;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByUserId(Integer userId) {
        return cardRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(userId).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        return CardResponse.from(card);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardByUuid(String uuid) {
        Card card = cardRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        return CardResponse.from(card);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getMyCards(UserPrincipal principal) {
        return cardRepository.findAllByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(principal.id()).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse getMyLatestCard(UserPrincipal principal) {
        Card card = cardRepository.findTopByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(principal.id())
                .orElse(null);
        return null == card ? null : CardResponse.from(card);
    }

    @Transactional
    public CardCreateResult createMyCard(UserPrincipal principal, CardRequest request) {
        Optional<Card> duplicate = StringUtils.hasText(request.position())
                ? cardRepository.findFirstByUserIdAndDeletedAtIsNullAndNameAndEmailAndCompanyAndPositionOrderByCreatedAtDesc(
                        principal.id(), request.name(), request.email(), request.company(), request.position())
                : cardRepository.findFirstByUserIdAndDeletedAtIsNullAndNameAndEmailAndCompanyOrderByCreatedAtDesc(
                        principal.id(), request.name(), request.email(), request.company());

        if (duplicate.isPresent()) {
            return new CardCreateResult(CardResponse.from(duplicate.get()), ResultType.DUPLICATE);
        }

        Optional<Card> anonymous = StringUtils.hasText(request.position())
                ? cardRepository.findFirstByUserIsNullAndDeletedAtIsNullAndNameAndEmailAndCompanyAndPositionOrderByCreatedAtDesc(
                        request.name(), request.email(), request.company(), request.position())
                : cardRepository.findFirstByUserIsNullAndDeletedAtIsNullAndNameAndEmailAndCompanyOrderByCreatedAtDesc(
                        request.name(), request.email(), request.company());

        User user = userRepository.getReferenceById(principal.id());

        if (anonymous.isPresent()) {
            anonymous.get().setUser(user);
            return new CardCreateResult(CardResponse.from(anonymous.get()), ResultType.CLAIMED);
        }

        Card card = Card.create(
                user,
                Card.newUuid(),
                request.name(),
                request.email(),
                request.phone_number(),
                request.lined_number(),
                request.company(),
                request.position(),
                request.department(),
                request.start_date(),
                request.end_date(),
                request.ai_image_key()
        );
        Card saved = cardRepository.save(card);

        if (hasJobInfo(saved)) {
            eventPublisher.publishEvent(new CardInfoUpdatedEvent(
                    saved.getId(), "CARD", LocalDateTime.now()
            ));
        }

        return new CardCreateResult(CardResponse.from(saved), ResultType.CREATED);
    }

    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#cardId, authentication)")
    public CardResponse updateMyCard(
            UserPrincipal principal,
            Integer cardId,
            Map<String, Object> request
    ) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        if (request == null) {
            return CardResponse.from(card);
        }

        applyUpdates(card, request);

        if (hasJobInfo(card)) {
            eventPublisher.publishEvent(new CardInfoUpdatedEvent(
                    card.getId(), "CARD", LocalDateTime.now()
            ));
        }

        return CardResponse.from(card);
    }

    private void applyUpdates(Card card, Map<String, Object> request) {
        applyIfPresent(request, "name", value -> {
            if (!value.matches("^[^\\p{P}\\p{S}\\p{Z}\\p{N}\\p{C}]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름 형식이 올바르지 않습니다");
            }
            card.updateName(value);
        });
        applyIfPresent(request, "email", card::updateEmail);
        applyIfPresent(request, "phone_number", value -> {
            if (!value.matches("^010-\\d{4}-\\d{4}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전화번호 형식이 올바르지 않습니다");
            }
            card.updatePhoneNumber(value);
        });
        applyIfPresent(request, "lined_number", card::updateLinedNumber);
        applyIfPresent(request, "company", value -> {
            if (!value.matches("^[가-힣a-zA-Z0-9\\s()&.]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회사명 형식이 올바르지 않습니다");
            }
            card.updateCompany(value);
        });
        applyIfPresent(request, "position", card::updatePosition);
        applyIfPresent(request, "department", card::updateDepartment);
        applyDateIfPresent(request, "start_date", card::updateStartDate);
        applyIfPresent(request, "ai_image_key", card::updateAiImageKey);

        if (request.containsKey("is_progress")) {
            Boolean isProgress = (Boolean) request.get("is_progress");
            card.updateIsProgress(isProgress);
            if (Boolean.TRUE.equals(isProgress)) {
                card.updateEndDate(null);
            }
        }

        if (request.containsKey("end_date")) {
            Object value = request.get("end_date");
            if (value == null) {
                card.updateEndDate(null);
                card.updateIsProgress(Boolean.TRUE);
            } else {
                LocalDate endDate = value instanceof LocalDate ? (LocalDate) value : parseDate((String) value);
                card.updateEndDate(endDate);
                card.updateIsProgress(Boolean.FALSE);
            }
        }
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
                updater.accept(parseDate((String) value));
            }
        }
    }

    private boolean hasJobInfo(Card card) {
        return StringUtils.hasText(card.getCompany())
                && StringUtils.hasText(card.getPosition())
                && StringUtils.hasText(card.getDepartment());
    }

    private LocalDate parseDate(String value) {
        if (value.matches("\\d{4}-\\d{2}")) {
            return YearMonth.parse(value).atDay(1);
        }
        return LocalDate.parse(value);
    }

    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#cardId, authentication)")
    public void deleteMyCard(UserPrincipal principal, Integer cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        cardRepository.delete(card);
    }
}
