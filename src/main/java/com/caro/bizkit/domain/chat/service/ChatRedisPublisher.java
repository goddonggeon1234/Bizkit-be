package com.caro.bizkit.domain.chat.service;

import com.caro.bizkit.domain.chat.dto.ChatMessageResponse;
import com.caro.bizkit.domain.chat.dto.ChatReadEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic chatMessageTopic;
    private final ChannelTopic chatReadTopic;
    private final ObjectMapper objectMapper;

    public void publish(ChatMessageResponse message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(chatMessageTopic.getTopic(), json);
        } catch (JsonProcessingException e) {
            log.error("Redis publish 직렬화 실패: roomId={}, messageId={}", message.room_id(), message.message_id(), e);
        }
    }

    public void publishReadNotification(ChatReadEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.debug("[READ] Redis 발행 - channel={}, roomId={}, targetUserId={}, messageId={}",
                    chatReadTopic.getTopic(), event.room_id(), event.target_user_id(), event.last_read_message_id());
            redisTemplate.convertAndSend(chatReadTopic.getTopic(), json);
            log.debug("[READ] Redis 발행 완료 - roomId={}, targetUserId={}", event.room_id(), event.target_user_id());
        } catch (JsonProcessingException e) {
            log.error("Redis read notification publish 실패: roomId={}, targetUserId={}", event.room_id(), event.target_user_id(), e);
        }
    }
}
