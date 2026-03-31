package com.caro.bizkit.domain.card.repository;

import com.caro.bizkit.domain.card.entity.Card;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Integer> {
    List<Card> findAllByUserId(Integer userId);
    Optional<Card> findByUuid(String uuid);
    Optional<Card> findTopByUserIdOrderByCreatedAtDesc(Integer userId);
    List<Card> findAllByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(Integer userId);
    Optional<Card> findTopByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(Integer userId);

    // OCR 등록 — position 있는 경우
    Optional<Card> findFirstByDeletedAtIsNullAndNameAndEmailAndCompanyAndPositionOrderByCreatedAtDesc(
            String name, String email, String company, String position);

    // OCR 등록 — position 없는 경우
    Optional<Card> findFirstByDeletedAtIsNullAndNameAndEmailAndCompanyOrderByCreatedAtDesc(
            String name, String email, String company);

    // 회원가입 — 익명 카드 전체 매칭
    List<Card> findAllByUserIsNullAndDeletedAtIsNullAndNameAndEmail(
            String name, String email);

    // 내 명함 생성 중복 확인 — position 있는 경우
    Optional<Card> findFirstByUserIdAndDeletedAtIsNullAndNameAndEmailAndCompanyAndPositionOrderByCreatedAtDesc(
            Integer userId, String name, String email, String company, String position);

    // 내 명함 생성 중복 확인 — position 없는 경우
    Optional<Card> findFirstByUserIdAndDeletedAtIsNullAndNameAndEmailAndCompanyOrderByCreatedAtDesc(
            Integer userId, String name, String email, String company);

    // 익명 명함 귀속 — position 있는 경우
    Optional<Card> findFirstByUserIsNullAndDeletedAtIsNullAndNameAndEmailAndCompanyAndPositionOrderByCreatedAtDesc(
            String name, String email, String company, String position);

    // 익명 명함 귀속 — position 없는 경우
    Optional<Card> findFirstByUserIsNullAndDeletedAtIsNullAndNameAndEmailAndCompanyOrderByCreatedAtDesc(
            String name, String email, String company);
}
