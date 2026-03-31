package com.caro.bizkit.domain.chat.controller;

import com.caro.bizkit.domain.chat.dto.ChatMessageRequest;
import com.caro.bizkit.domain.chat.dto.ChatMessageResponse;
import com.caro.bizkit.domain.chat.service.ChatMessageService;
import com.caro.bizkit.domain.chat.service.ChatRedisPublisher;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.security.UserPrincipalCacheService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final ChatRedisPublisher chatRedisPublisher;
    private final UserPrincipalCacheService userPrincipalCacheService;

    @MessageMapping("/chat/messages")
    public void sendMessage(ChatMessageRequest request, Principal principal) {
        Integer userId = Integer.valueOf(principal.getName());

        UserPrincipal userPrincipal = userPrincipalCacheService.findById(userId);
        if (userPrincipal == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // DB 저장
        ChatMessageResponse response = chatMessageService.sendMessage(userPrincipal, request);

        // Redis publish (모든 인스턴스에 브로드캐스트)
        chatRedisPublisher.publish(response);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception ex) {
        log.warn("STOMP 메시지 처리 에러: {}", ex.getMessage());
        return ex.getMessage() != null ? ex.getMessage() : "메시지 처리 중 오류가 발생했습니다.";
    }
}
