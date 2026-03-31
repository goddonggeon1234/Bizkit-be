package com.caro.bizkit.domain.chat.repository;

import com.caro.bizkit.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.createdAt >= :joinedAt ORDER BY m.id DESC")
    List<ChatMessage> findFirstPage(@Param("roomId") Integer roomId,
                                    @Param("joinedAt") LocalDateTime joinedAt,
                                    Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.id < :cursorId AND m.createdAt >= :joinedAt ORDER BY m.id DESC")
    List<ChatMessage> findByCursor(@Param("roomId") Integer roomId,
                                   @Param("cursorId") Long cursorId,
                                   @Param("joinedAt") LocalDateTime joinedAt,
                                   Pageable pageable);

    @Query("SELECT cp.chatRoom.id, COUNT(m) FROM ChatParticipant cp " +
            "JOIN ChatMessage m ON m.chatRoom.id = cp.chatRoom.id " +
            "AND m.createdAt >= cp.joinedAt " +
            "AND (cp.lastReadMessageId IS NULL OR m.id > cp.lastReadMessageId) " +
            "AND m.participant.user.id <> cp.user.id " +
            "WHERE cp.user.id = :userId AND cp.leftAt IS NULL " +
            "GROUP BY cp.chatRoom.id")
    List<Object[]> countUnreadBatch(@Param("userId") Integer userId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoom.id = :roomId " +
            "AND m.createdAt >= :joinedAt " +
            "AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId) " +
            "AND m.participant.user.id <> :userId")
    int countUnreadMessages(@Param("roomId") Integer roomId,
                            @Param("lastReadMessageId") Long lastReadMessageId,
                            @Param("joinedAt") LocalDateTime joinedAt,
                            @Param("userId") Integer userId);

    Optional<ChatMessage> findTopByChatRoomIdOrderByIdDesc(Integer chatRoomId);
}
