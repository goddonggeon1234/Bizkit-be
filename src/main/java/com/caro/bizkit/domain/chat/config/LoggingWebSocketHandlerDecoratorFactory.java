package com.caro.bizkit.domain.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Slf4j
@Component
public class LoggingWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                log.info("WebSocket 연결 성공: sessionId={}, uri={}, remoteAddress={}",
                        session.getId(),
                        session.getUri(),
                        session.getRemoteAddress()
                );
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                if (closeStatus.equalsCode(CloseStatus.NORMAL)) {
                    log.info("WebSocket 정상 종료: sessionId={}, status={}", session.getId(), closeStatus);
                } else {
                    log.warn("WebSocket 비정상 종료: sessionId={}, status={}", session.getId(), closeStatus);
                }
                super.afterConnectionClosed(session, closeStatus);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.error("WebSocket 전송 오류: sessionId={}, error={}", session.getId(), exception.getMessage(), exception);
                super.handleTransportError(session, exception);
            }
        };
    }
}
