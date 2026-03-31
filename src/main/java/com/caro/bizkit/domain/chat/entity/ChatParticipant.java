package com.caro.bizkit.domain.chat.entity;

import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_participant", indexes = {
        @Index(name = "idx_chat_participant_user_room", columnList = "user_id, room_id"),
        @Index(name = "idx_chat_participant_room_left", columnList = "room_id, left_at")
})
public class ChatParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    private Long lastReadMessageId;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @Column(nullable = false)
    private Integer unreadCount = 0;

    public static ChatParticipant create(User user, ChatRoom chatRoom) {
        ChatParticipant participant = new ChatParticipant();
        participant.user = user;
        participant.chatRoom = chatRoom;
        participant.joinedAt = LocalDateTime.now();
        return participant;
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    public boolean hasLeft() {
        return this.leftAt != null;
    }

    public void rejoin() {
        this.joinedAt = LocalDateTime.now();
        this.leftAt = null;
        this.lastReadMessageId = null;
    }

    public void updateLastReadMessageId(Long messageId) {
        this.lastReadMessageId = messageId;
    }
}
