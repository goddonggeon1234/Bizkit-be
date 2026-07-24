package com.caro.bizkit.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_room")
public class ChatRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 2000)
    private String latestMessageContent;

    private LocalDateTime latestMessageCreatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static ChatRoom create() {
        ChatRoom room = new ChatRoom();
        room.createdAt = LocalDateTime.now();
        return room;
    }

    public void updateLatestMessage(String content, LocalDateTime createdAt) {
        this.latestMessageContent = content;
        this.latestMessageCreatedAt = createdAt;
    }
}
