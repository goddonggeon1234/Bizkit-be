package com.caro.bizkit.domain.userdetail.link.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.userdetail.link.dto.LinkRequest;
import com.caro.bizkit.domain.userdetail.link.dto.LinkResponse;
import java.util.Map;
import com.caro.bizkit.domain.userdetail.link.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
@Tag(name = "Link", description = "링크 정보 조회 API")
public class LinkController {

    private final LinkService linkService;

    @GetMapping("/me")
    @Operation(summary = "내 링크 조회", description = "인증된 사용자의 링크 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<LinkResponse>>> getMyLinks(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<LinkResponse> links = linkService.getMyLinks(user);
        return ResponseEntity.ok(ApiResponse.success("내 링크 조회 성공", links));
    }

    @GetMapping
    @Operation(summary = "사용자 링크 조회", description = "지정된 사용자 링크 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<LinkResponse>>> getLinksByUserId(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("userId") Integer userId
    ) {
        List<LinkResponse> links = linkService.getLinksByUserId(principal, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 링크 조회 성공", links));
    }

    @PostMapping("/me")
    @Operation(summary = "내 링크 생성", description = "인증된 사용자의 링크를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<LinkResponse>> createMyLink(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody LinkRequest request
    ) {
        LinkResponse link = linkService.createMyLink(user, request);
        return ResponseEntity.ok(ApiResponse.success("내 링크 생성 성공", link));
    }

    @PatchMapping("/{link_id}")
    @Operation(summary = "내 링크 수정", description = "링크 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<LinkResponse>> updateMyLink(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("link_id") Integer linkId,
            @RequestBody Map<String, Object> request
    ) {
        LinkResponse link = linkService.updateMyLink(user, linkId, request);
        return ResponseEntity.ok(ApiResponse.success("내 링크 수정 성공", link));
    }

    @DeleteMapping("/{link_id}")
    @Operation(summary = "내 링크 삭제", description = "링크 정보를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMyLink(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("link_id") Integer linkId
    ) {
        linkService.deleteMyLink(user, linkId);
        return ResponseEntity.noContent().build();
    }
}
