package com.caro.bizkit.domain.chat.service;

import com.caro.bizkit.domain.chat.dto.ChatMessagePagination;
import com.caro.bizkit.domain.chat.dto.ChatMessageRequest;
import com.caro.bizkit.domain.chat.dto.ChatMessageResponse;
import com.caro.bizkit.domain.chat.dto.ChatMessagesResult;
import com.caro.bizkit.domain.chat.dto.ChatReadEvent;
import com.caro.bizkit.domain.chat.entity.ChatMessage;
import com.caro.bizkit.domain.chat.entity.ChatParticipant;
import com.caro.bizkit.domain.chat.entity.ChatRoom;
import com.caro.bizkit.domain.chat.repository.ChatMessageRepository;
import com.caro.bizkit.domain.chat.repository.ChatParticipantRepository;
import com.caro.bizkit.domain.chat.repository.ChatRoomRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRedisPublisher chatRedisPublisher;

    @Transactional
    public ChatMessageResponse sendMessage(UserPrincipal principal, ChatMessageRequest request) {
        ChatParticipant participant = chatParticipantRepository
                .findByUserIdAndChatRoomId(principal.id(), request.room_id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "채팅방 참여자가 아닙니다."));

        if (participant.hasLeft()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "나간 채팅방에는 메시지를 보낼 수 없습니다.");
        }

        ChatRoom room = participant.getChatRoom();
        ChatMessage message = ChatMessage.create(room, participant, request.content());
        chatMessageRepository.save(message);

        room.updateLatestMessage(message.getContent(), message.getCreatedAt());

        return ChatMessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public ChatMessagesResult getMessages(UserPrincipal principal, Integer roomId, Long cursorId, int size) {
        ChatParticipant participant = chatParticipantRepository
                .findByUserIdAndChatRoomId(principal.id(), roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "채팅방 참여자가 아닙니다."));

        // limit + 1로 조회하여 has_next 판단
        PageRequest pageable = PageRequest.of(0, size + 1);
        List<ChatMessage> messages;

        if (cursorId == null) {
            messages = chatMessageRepository.findFirstPage(roomId, participant.getJoinedAt(), pageable);
        } else {
            messages = chatMessageRepository.findByCursor(roomId, cursorId, participant.getJoinedAt(), pageable);
        }

        // size + 1 개를 요청했으므로, size 초과분이 있으면 has_next = true
        List<ChatMessage> result = messages.size() > size ? messages.subList(0, size) : messages;
        List<ChatMessageResponse> responses = result.stream().map(ChatMessageResponse::from).toList();

        // 상대방의 lastReadMessageId 조회
        Long otherLastReadMessageId = chatParticipantRepository
                .findByChatRoomIdAndLeftAtIsNull(roomId)
                .stream()
                .filter(p -> !p.getUser().getId().equals(principal.id()))
                .findFirst()
                .map(ChatParticipant::getLastReadMessageId)
                .orElse(null);

        return new ChatMessagesResult(responses, otherLastReadMessageId);
    }

    public ChatMessagePagination buildPagination(ChatMessagesResult result, int requestedSize) {
        boolean hasNext = result.messages().size() == requestedSize;
        Long cursorId = result.messages().isEmpty() ? null : result.messages().getLast().message_id();
        return new ChatMessagePagination(cursorId, hasNext);
    }

    @Transactional
    public void markAsRead(UserPrincipal principal, Integer roomId) {
        ChatParticipant participant = chatParticipantRepository
                .findByUserIdAndChatRoomId(principal.id(), roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "채팅방 참여자가 아닙니다."));

        if (participant.hasLeft()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "나간 채팅방입니다.");
        }

        chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId).ifPresent(latest -> {
            log.debug("[READ] markAsRead - roomId={}, userId={}, latestMessageId={}", roomId, principal.id(), latest.getId());
            participant.updateLastReadMessageId(latest.getId());

            // 상대방에게 읽음 알림 전송 (Redis pub/sub으로 멀티 인스턴스 지원)
            chatParticipantRepository.findByChatRoomIdAndLeftAtIsNull(roomId).stream()
                    .filter(p -> !p.getUser().getId().equals(principal.id()))
                    .findFirst()
                    .ifPresentOrElse(
                            other -> {
                                log.debug("[READ] 상대방 발견 - targetUserId={}, roomId={}", other.getUser().getId(), roomId);
                                chatRedisPublisher.publishReadNotification(
                                        new ChatReadEvent(roomId, latest.getId(), other.getUser().getId())
                                );
                            },
                            () -> log.warn("[READ] 상대방 없음 - roomId={}, userId={}", roomId, principal.id())
                    );
        });
    }
}
