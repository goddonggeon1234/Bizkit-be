package com.caro.bizkit.domain.chat.config;

import com.caro.bizkit.domain.auth.service.WsTicketService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    private static final String USER_ID_ATTR = "userId";
    private static final String TOKEN_ATTR = "accessToken";

    private final WsTicketService wsTicketService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 1. ticket 기반 인증 (BFF 방식)
            String ticket = httpRequest.getParameter("ticket");
            if (ticket != null) {
                Integer userId = wsTicketService.validateAndConsume(ticket);
                if (userId != null) {
                    attributes.put(USER_ID_ATTR, userId);
                    log.debug("WebSocket handshake: ticket 인증 성공, userId={}", userId);
                    return true;
                }
                log.warn("WebSocket handshake: 유효하지 않거나 만료된 ticket");
                return false;
            }

            // 2. 쿠키 폴백 (기존 방식)
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                Arrays.stream(cookies)
                        .filter(c -> TOKEN_ATTR.equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .ifPresent(token -> attributes.put(TOKEN_ATTR, token));
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        if (exception != null) {
            log.error("WebSocket handshake 실패: uri={}, error={}", request.getURI(), exception.getMessage(), exception);
        }
    }
}
