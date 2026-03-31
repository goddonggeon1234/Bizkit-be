package com.caro.bizkit.domain.chat.entity;

import io.hypersistence.tsid.TSID;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_chat_message_room_id", columnList = "room_id, id")
})
public class ChatMessage {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ChatParticipant participant;

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static ChatMessage create(ChatRoom chatRoom, ChatParticipant participant, String content) {
        ChatMessage message = new ChatMessage();
        message.id = TSID.Factory.getTsid().toLong();
        message.chatRoom = chatRoom;
        message.participant = participant;
        message.content = content;
        message.createdAt = LocalDateTime.now();
        return message;
    }
}
