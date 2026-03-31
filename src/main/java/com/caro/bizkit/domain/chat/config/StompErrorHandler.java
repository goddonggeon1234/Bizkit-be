package com.caro.bizkit.domain.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Slf4j
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
            Message<byte[]> clientMessage, Throwable ex
    ) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String errorMessage = cause.getMessage() != null ? cause.getMessage() : "알 수 없는 오류가 발생했습니다.";

        log.warn("STOMP 에러: {}", errorMessage);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorMessage);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                errorMessage.getBytes(),
                accessor.getMessageHeaders()
        );
    }
}
