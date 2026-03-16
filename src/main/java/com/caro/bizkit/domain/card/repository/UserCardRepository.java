package com.caro.bizkit.domain.card.repository;

import com.caro.bizkit.domain.card.entity.UserCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCardRepository extends JpaRepository<UserCard, Integer> {
    boolean existsByUserIdAndCardId(Integer userId, Integer cardId);
    List<UserCard> findAllByUserId(Integer userId);
    Optional<UserCard> findByUserIdAndCardId(Integer userId, Integer cardId);
    @EntityGraph(attributePaths = {"card", "card.user"})
    List<UserCard> findByUserIdOrderByIdDesc(Integer userId, Pageable pageable);

    @EntityGraph(attributePaths = {"card", "card.user"})
    List<UserCard> findByUserIdAndIdLessThanOrderByIdDesc(Integer userId, Integer cursorId, Pageable pageable);

    @Query(value = """
            SELECT uc.id
            FROM user_card uc USE INDEX (idx_user_card_user_id_id)
            JOIN card c ON c.id = uc.card_id
            WHERE uc.user_id = :userId
              AND (
                  c.name LIKE CONCAT('%', :keyword, '%')
               OR c.company LIKE CONCAT('%', :keyword, '%')
               OR c.email LIKE CONCAT('%', :keyword, '%')
               OR c.position LIKE CONCAT('%', :keyword, '%')
               OR c.phone_number LIKE CONCAT('%', :keyword, '%')
               OR c.department LIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY uc.id DESC
            """, nativeQuery = true)
    List<Integer> searchCollectedCardIds(
            @Param("userId") Integer userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(value = """
            SELECT uc.id
            FROM user_card uc USE INDEX (idx_user_card_user_id_id)
            JOIN card c ON c.id = uc.card_id
            WHERE uc.user_id = :userId
              AND uc.id < :cursorId
              AND (
                  c.name LIKE CONCAT('%', :keyword, '%')
               OR c.company LIKE CONCAT('%', :keyword, '%')
               OR c.email LIKE CONCAT('%', :keyword, '%')
               OR c.position LIKE CONCAT('%', :keyword, '%')
               OR c.phone_number LIKE CONCAT('%', :keyword, '%')
               OR c.department LIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY uc.id DESC
            """, nativeQuery = true)
    List<Integer> searchCollectedCardIdsWithCursor(
            @Param("userId") Integer userId,
            @Param("cursorId") Integer cursorId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT uc FROM UserCard uc
            JOIN FETCH uc.card c
            LEFT JOIN FETCH c.user
            WHERE uc.id IN :ids
            ORDER BY uc.id DESC
            """)
    List<UserCard> findAllByIdInWithFetch(@Param("ids") List<Integer> ids);

    void deleteAllByUserId(Integer userId);

    /**
     * 특정 사용자(collectorId)가 수집한 카드 중에서
     * 카드 주인이 cardOwnerId인 카드가 있는지 확인
     */
    @Query("""
            SELECT CASE WHEN COUNT(uc) > 0 THEN true ELSE false END
            FROM UserCard uc
            WHERE uc.user.id = :collectorId
              AND uc.card.user.id = :cardOwnerId
            """)
    boolean existsCollectedCardByOwner(
            @Param("collectorId") Integer collectorId,
            @Param("cardOwnerId") Integer cardOwnerId
    );
}
