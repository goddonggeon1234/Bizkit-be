package com.caro.bizkit.domain.chat.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.chat.dto.ChatMessagePagination;
import com.caro.bizkit.domain.chat.dto.ChatMessageResponse;
import com.caro.bizkit.domain.chat.dto.ChatMessagesResult;
import com.caro.bizkit.domain.chat.service.ChatMessageService;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms/{room_id}")
@RequiredArgsConstructor
@Tag(name = "Chat Message", description = "채팅 메시지 API")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/messages")
    @Operation(summary = "메시지 이력 조회", description = "커서 기반 페이지네이션으로 메시지를 조회합니다. joinedAt 이후 메시지만 반환됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "message": "메시지 조회 성공",
                              "data": [
                                {
                                  "message_id": 1,
                                  "room_id": 1,
                                  "sender_user_id": 10,
                                  "sender_name": "홍길동",
                                  "content": "안녕하세요!",
                                  "created_at": "2026-02-22T10:30:00"
                                }
                              ],
                              "pagination": {
                                "cursorId": 1,
                                "has_next": true
                              }
                            }
                            """)))
    })
    public ResponseEntity<ApiResponse<ChatMessagesResult>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("room_id") Integer roomId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size
    ) {
        ChatMessagesResult result = chatMessageService.getMessages(principal, roomId, cursorId, size);
        ChatMessagePagination pagination = chatMessageService.buildPagination(result, size);
        return ResponseEntity.ok(ApiResponse.successWithPagination("메시지 조회 성공", result, pagination));
    }

    @PostMapping("/read")
    @Operation(summary = "읽음 처리", description = "해당 채팅방의 최신 메시지까지 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("room_id") Integer roomId
    ) {
        chatMessageService.markAsRead(principal, roomId);
        return ResponseEntity.ok(ApiResponse.success("읽음 처리 성공", null));
    }
}
