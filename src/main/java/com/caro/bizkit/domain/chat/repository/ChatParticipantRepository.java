package com.caro.bizkit.domain.chat.repository;

import com.caro.bizkit.domain.chat.entity.ChatParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Integer> {

    List<ChatParticipant> findByUserIdAndLeftAtIsNull(Integer userId);

    Optional<ChatParticipant> findByUserIdAndChatRoomId(Integer userId, Integer chatRoomId);

    List<ChatParticipant> findByChatRoomIdAndLeftAtIsNull(Integer chatRoomId);

    boolean existsByUserIdAndChatRoomIdAndLeftAtIsNull(Integer userId, Integer chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.chatRoom.id IN " +
            "(SELECT cp2.chatRoom.id FROM ChatParticipant cp2 WHERE cp2.user.id = :userId1) " +
            "AND cp.user.id = :userId2")
    Optional<ChatParticipant> findCommonRoomParticipant(@Param("userId1") Integer userId1,
                                                        @Param("userId2") Integer userId2);

    @Query("SELECT CASE WHEN EXISTS (" +
            "SELECT cp1 FROM ChatParticipant cp1 " +
            "JOIN ChatParticipant cp2 ON cp1.chatRoom.id = cp2.chatRoom.id " +
            "WHERE cp1.user.id = :userId1 " +
            "AND cp2.user.id = :userId2" +
            ") THEN true ELSE false END")
    boolean existsSharedRoom(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "JOIN FETCH cp.chatRoom " +
            "WHERE cp.user.id = :userId AND cp.leftAt IS NULL " +
            "ORDER BY cp.chatRoom.latestMessageCreatedAt DESC NULLS LAST, cp.chatRoom.id DESC")
    List<ChatParticipant> findMyRooms(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT cp FROM ChatParticipant cp " +
            "JOIN FETCH cp.chatRoom " +
            "WHERE cp.user.id = :userId AND cp.leftAt IS NULL " +
            "AND (" +
            "  (cp.chatRoom.latestMessageCreatedAt < (SELECT cr.latestMessageCreatedAt FROM ChatRoom cr WHERE cr.id = :cursorId)) " +
            "  OR (cp.chatRoom.latestMessageCreatedAt = (SELECT cr.latestMessageCreatedAt FROM ChatRoom cr WHERE cr.id = :cursorId) AND cp.chatRoom.id < :cursorId) " +
            "  OR (cp.chatRoom.latestMessageCreatedAt IS NULL AND (SELECT cr.latestMessageCreatedAt FROM ChatRoom cr WHERE cr.id = :cursorId) IS NOT NULL) " +
            "  OR (cp.chatRoom.latestMessageCreatedAt IS NULL AND (SELECT cr.latestMessageCreatedAt FROM ChatRoom cr WHERE cr.id = :cursorId) IS NULL AND cp.chatRoom.id < :cursorId) " +
            ") " +
            "ORDER BY cp.chatRoom.latestMessageCreatedAt DESC NULLS LAST, cp.chatRoom.id DESC")
    List<ChatParticipant> findMyRoomsWithCursor(@Param("userId") Integer userId,
                                                 @Param("cursorId") Integer cursorId,
                                                 Pageable pageable);
}
