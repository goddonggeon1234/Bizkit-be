package com.caro.bizkit.domain.chat.dto;

import com.caro.bizkit.domain.chat.entity.ChatParticipant;
import com.caro.bizkit.domain.chat.entity.ChatRoom;
import com.caro.bizkit.domain.user.entity.User;
import java.time.LocalDateTime;

public record ChatRoomResponse(
        Integer room_id,
        Integer other_user_id,
        String other_user_name,
        String other_user_profile_image_url,
        String latest_message_content,
        LocalDateTime latest_message_created_at,
        int unread_count
) {
    public static ChatRoomResponse from(ChatRoom room, User otherUser, String cardName, int unreadCount) {
        return new ChatRoomResponse(
                room.getId(),
                otherUser.getId(),
                cardName != null ? cardName : otherUser.getName(),
                otherUser.getProfileImageKey(),
                room.getLatestMessageContent(),
                room.getLatestMessageCreatedAt(),
                unreadCount
        );
    }
}
