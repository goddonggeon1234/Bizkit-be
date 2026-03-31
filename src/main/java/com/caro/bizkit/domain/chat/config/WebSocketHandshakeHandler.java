package com.caro.bizkit.domain.chat.config;

import com.caro.bizkit.security.JwtTokenProvider;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        // 1. ticket 기반: HttpHandshakeInterceptor에서 이미 userId 저장됨
        Object userId = attributes.get("userId");
        if (userId != null) {
            String name = String.valueOf(userId);
            log.debug("WebSocket principal 설정: userId={} (ticket)", name);
            return new StompPrincipal(name);
        }

        // 2. cookie 기반 JWT
        Object token = attributes.get("accessToken");
        if (token != null) {
            String tokenStr = token.toString();
            if (jwtTokenProvider.isValid(tokenStr)) {
                String name = jwtTokenProvider.parseClaims(tokenStr).getSubject();
                log.debug("WebSocket principal 설정: userId={} (JWT)", name);
                return new StompPrincipal(name);
            }
            log.warn("WebSocket principal 설정 실패: JWT 유효하지 않음");
        }

        return null;
    }
}
