package com.caro.bizkit.common.config;

import com.caro.bizkit.domain.ai.service.SseEmitterService;
import com.caro.bizkit.domain.chat.service.ChatRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public ChannelTopic chatMessageTopic() {
        return new ChannelTopic("chat:messages");
    }

    @Bean
    public ChannelTopic chatReadTopic() {
        return new ChannelTopic("chat:read");
    }

    @Bean
    public ChannelTopic sseNotificationTopic() {
        return new ChannelTopic("sse:notifications");
    }

    @Bean
    public MessageListenerAdapter chatMessageListenerAdapter(ChatRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public MessageListenerAdapter chatReadListenerAdapter(ChatRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onReadNotification");
    }

    @Bean
    public MessageListenerAdapter sseNotificationListenerAdapter(SseEmitterService sseEmitterService) {
        return new MessageListenerAdapter(sseEmitterService, "handleSseMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter chatMessageListenerAdapter,
            ChannelTopic chatMessageTopic,
            MessageListenerAdapter chatReadListenerAdapter,
            ChannelTopic chatReadTopic,
            MessageListenerAdapter sseNotificationListenerAdapter,
            ChannelTopic sseNotificationTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatMessageListenerAdapter, chatMessageTopic);
        container.addMessageListener(chatReadListenerAdapter, chatReadTopic);
        container.addMessageListener(sseNotificationListenerAdapter, sseNotificationTopic);
        return container;
    }
}
