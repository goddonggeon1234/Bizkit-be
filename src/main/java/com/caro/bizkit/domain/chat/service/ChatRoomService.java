package com.caro.bizkit.domain.chat.service;

import com.caro.bizkit.common.S3.service.S3Service;
import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.chat.dto.ChatPartnerProfileResponse;
import com.caro.bizkit.domain.chat.dto.ChatRoomCreateRequest;
import com.caro.bizkit.domain.chat.dto.ChatRoomListResult;
import com.caro.bizkit.domain.chat.dto.ChatRoomResponse;
import com.caro.bizkit.domain.chat.entity.ChatParticipant;
import com.caro.bizkit.domain.chat.entity.ChatRoom;
import com.caro.bizkit.domain.card.entity.Card;
import com.caro.bizkit.domain.card.repository.CardRepository;
import com.caro.bizkit.domain.chat.repository.ChatMessageRepository;
import com.caro.bizkit.domain.chat.repository.ChatParticipantRepository;
import com.caro.bizkit.domain.chat.repository.ChatRoomRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final S3Service s3Service;

    private static final String ROOM_LOCK_PREFIX = "chat:room:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    @Transactional
    public ChatRoomResponse createRoom(UserPrincipal principal, ChatRoomCreateRequest request) {
        Integer myId = principal.id();
        Integer targetId = request.target_user_id();

        if (myId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신과 채팅할 수 없습니다.");
        }

        User targetUser = userRepository.findByIdAndDeletedAtIsNull(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 사용자를 찾을 수 없습니다."));

        String targetCardName = getLatestCardName(targetId);

        // 기존 방 조회 (나간 사람 포함)
        ChatParticipant existing = chatParticipantRepository.findCommonRoomParticipant(myId, targetId)
                .orElse(null);

        if (existing != null) {
            ChatRoom room = existing.getChatRoom();
            ChatParticipant myParticipant = chatParticipantRepository
                    .findByUserIdAndChatRoomId(myId, room.getId())
                    .orElseThrow();

            if (myParticipant.hasLeft()) {
                myParticipant.rejoin();
            }
            return ChatRoomResponse.from(room, targetUser, targetCardName, 0);
        }

        // Redis 분산 락으로 중복 생성 방지
        int minId = Math.min(myId, targetId);
        int maxId = Math.max(myId, targetId);
        String lockKey = ROOM_LOCK_PREFIX + minId + ":" + maxId;

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (acquired == null || !acquired) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "채팅방 생성 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            // 락 획득 후 다시 확인 (다른 인스턴스가 먼저 생성했을 수 있음)
            ChatParticipant doubleCheck = chatParticipantRepository.findCommonRoomParticipant(myId, targetId)
                    .orElse(null);
            if (doubleCheck != null) {
                ChatRoom room = doubleCheck.getChatRoom();
                ChatParticipant myParticipant = chatParticipantRepository
                        .findByUserIdAndChatRoomId(myId, room.getId())
                        .orElseThrow();
                if (myParticipant.hasLeft()) {
                    myParticipant.rejoin();
                }
                return ChatRoomResponse.from(room, targetUser, targetCardName, 0);
            }

            User myUser = userRepository.findByIdAndDeletedAtIsNull(myId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

            ChatRoom room = ChatRoom.create();
            chatRoomRepository.save(room);

            ChatParticipant myParticipant = ChatParticipant.create(myUser, room);
            ChatParticipant targetParticipant = ChatParticipant.create(targetUser, room);
            chatParticipantRepository.save(myParticipant);
            chatParticipantRepository.save(targetParticipant);

            return ChatRoomResponse.from(room, targetUser, targetCardName, 0);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public ChatRoomListResult getMyRooms(UserPrincipal principal, Integer size, Integer cursorId) {
        Integer myId = principal.id();
        int limit = normalizeSize(size);

        List<ChatParticipant> myParticipants = findMyParticipants(myId, cursorId, limit + 1);

        boolean hasNext = myParticipants.size() > limit;
        if (hasNext) {
            myParticipants = myParticipants.subList(0, limit);
        }

        if (myParticipants.isEmpty()) {
            return new ChatRoomListResult(List.of(), null, false);
        }

        // 배치 COUNT 쿼리로 unread 일괄 조회
        Map<Integer, Integer> unreadMap = new HashMap<>();
        List<Object[]> unreadResults = chatMessageRepository.countUnreadBatch(myId);
        for (Object[] row : unreadResults) {
            Integer roomId = (Integer) row[0];
            Long count = (Long) row[1];
            unreadMap.put(roomId, count.intValue());
        }

        List<ChatRoomResponse> responses = new ArrayList<>();
        for (ChatParticipant myParticipant : myParticipants) {
            ChatRoom room = myParticipant.getChatRoom();

            // 상대방 찾기
            List<ChatParticipant> roomParticipants = chatParticipantRepository
                    .findByChatRoomIdAndLeftAtIsNull(room.getId());
            User otherUser = roomParticipants.stream()
                    .filter(p -> !p.getUser().getId().equals(myId))
                    .map(ChatParticipant::getUser)
                    .findFirst()
                    .orElse(null);

            if (otherUser == null) {
                continue;
            }

            String cardName = getLatestCardName(otherUser.getId());
            int unread = unreadMap.getOrDefault(room.getId(), 0);
            responses.add(ChatRoomResponse.from(room, otherUser, cardName, unread));
        }

        Integer nextCursorId = myParticipants.isEmpty() ? null : myParticipants.getLast().getChatRoom().getId();
        return new ChatRoomListResult(responses, nextCursorId, hasNext);
    }

    @Transactional
    public void leaveRoom(UserPrincipal principal, Integer roomId) {
        ChatParticipant participant = chatParticipantRepository
                .findByUserIdAndChatRoomId(principal.id(), roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방 참여 정보를 찾을 수 없습니다."));

        if (participant.hasLeft()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 나간 채팅방입니다.");
        }

        participant.leave();
    }

    @Transactional(readOnly = true)
    public ChatPartnerProfileResponse getPartnerProfile(UserPrincipal principal, Integer targetUserId) {
        Integer myId = principal.id();

        if (myId.equals(targetUserId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "자기 자신의 프로필은 조회할 수 없습니다");
        }

        User targetUser = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!chatParticipantRepository.existsSharedRoom(myId, targetUserId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "채팅 상대의 프로필을 조회할 권한이 없습니다");
        }

        String profileImageUrl = StringUtils.hasText(targetUser.getProfileImageKey())
                ? s3Service.getPublicUrl(targetUser.getProfileImageKey())
                : null;

        return ChatPartnerProfileResponse.from(targetUser, profileImageUrl);
    }

    private List<ChatParticipant> findMyParticipants(Integer userId, Integer cursorId, int limit) {
        if (cursorId == null) {
            return chatParticipantRepository.findMyRooms(userId, PageRequest.of(0, limit));
        }
        return chatParticipantRepository.findMyRoomsWithCursor(userId, cursorId, PageRequest.of(0, limit));
    }

    private String getLatestCardName(Integer userId) {
        return cardRepository.findTopByUserIdAndDeletedAtIsNullOrderByIsProgressDescStartDateDesc(userId)
                .map(Card::getName)
                .orElse(null);
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return size;
    }
}
