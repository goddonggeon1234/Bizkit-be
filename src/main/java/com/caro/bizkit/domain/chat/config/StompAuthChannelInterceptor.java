package com.caro.bizkit.domain.chat.config;

import com.caro.bizkit.domain.chat.repository.ChatParticipantRepository;
import com.caro.bizkit.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_ROOM_DESTINATION = Pattern.compile("^/sub/chat/rooms/(\\d+)$");
    private static final Pattern CHAT_READ_DESTINATION = Pattern.compile("^/sub/chat/read/(\\d+)$");
    private static final Pattern CHAT_NOTIFICATION_DESTINATION = Pattern.compile("^/sub/chat/notifications/(\\d+)$");

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            validateSubscription(accessor);
            return message;
        }

        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        var sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        // 1. ticket 기반 인증 (HttpHandshakeInterceptor에서 userId 세팅)
        Object userIdAttr = sessionAttributes.get("userId");
        if (userIdAttr != null) {
            String userId = String.valueOf(userIdAttr);
            accessor.setUser(new StompPrincipal(userId));
            log.info("STOMP CONNECT: ticket 기반 인증 성공, userId={}", userId);
            return message;
        }

        // 2. 쿠키 기반 JWT 폴백
        Object tokenAttr = sessionAttributes.get("accessToken");
        if (tokenAttr == null) {
            log.warn("STOMP CONNECT: 인증 정보 없음 (ticket/accessToken 모두 없음)");
            throw new IllegalArgumentException("인증 토큰이 없습니다.");
        }

        String token = tokenAttr.toString();
        if (!jwtTokenProvider.isValid(token)) {
            log.warn("STOMP CONNECT: invalid token");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        Claims claims = jwtTokenProvider.parseClaims(token);
        String userId = claims.getSubject();
        accessor.setUser(new StompPrincipal(userId));
        log.info("STOMP CONNECT: JWT 기반 인증 성공, userId={}", userId);

        return message;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Principal user = accessor.getUser();

        Matcher roomMatcher = CHAT_ROOM_DESTINATION.matcher(destination);
        if (roomMatcher.matches()) {
            if (user == null) {
                log.warn("STOMP SUBSCRIBE: 인증 정보 없음, destination={}", destination);
                throw new IllegalArgumentException("인증 정보가 없습니다.");
            }
            Integer userId = Integer.valueOf(user.getName());
            Integer roomId = Integer.valueOf(roomMatcher.group(1));
            boolean isMember = chatParticipantRepository.existsByUserIdAndChatRoomIdAndLeftAtIsNull(userId, roomId);
            if (!isMember) {
                log.warn("STOMP SUBSCRIBE: 채팅방 미참여자 구독 시도, userId={}, roomId={}", userId, roomId);
                throw new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다.");
            }
            log.debug("STOMP SUBSCRIBE: 멤버십 검증 성공, userId={}, roomId={}", userId, roomId);
            return;
        }

        Matcher readMatcher = CHAT_READ_DESTINATION.matcher(destination);
        if (readMatcher.matches()) {
            if (user == null) {
                log.warn("STOMP SUBSCRIBE: 인증 정보 없음, destination={}", destination);
                throw new IllegalArgumentException("인증 정보가 없습니다.");
            }
            Integer userId = Integer.valueOf(user.getName());
            Integer targetUserId = Integer.valueOf(readMatcher.group(1));
            if (!userId.equals(targetUserId)) {
                log.warn("STOMP SUBSCRIBE: 타인의 읽음 알림 구독 시도, userId={}, targetUserId={}", userId, targetUserId);
                throw new IllegalArgumentException("본인의 읽음 알림만 구독할 수 있습니다.");
            }
            log.debug("STOMP SUBSCRIBE: 읽음 알림 구독 검증 성공, userId={}", userId);
            return;
        }

        Matcher notifMatcher = CHAT_NOTIFICATION_DESTINATION.matcher(destination);
        if (notifMatcher.matches()) {
            if (user == null) {
                log.warn("STOMP SUBSCRIBE: 인증 정보 없음, destination={}", destination);
                throw new IllegalArgumentException("인증 정보가 없습니다.");
            }
            Integer userId = Integer.valueOf(user.getName());
            Integer targetUserId = Integer.valueOf(notifMatcher.group(1));
            if (!userId.equals(targetUserId)) {
                log.warn("STOMP SUBSCRIBE: 타인의 알림 구독 시도, userId={}, targetUserId={}", userId, targetUserId);
                throw new IllegalArgumentException("본인의 알림만 구독할 수 있습니다.");
            }
            log.debug("STOMP SUBSCRIBE: 알림 구독 검증 성공, userId={}", userId);
        }
    }
}
