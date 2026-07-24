package com.caro.bizkit.domain.userdetail.skill.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.userdetail.skill.dto.SkillResponse;
import com.caro.bizkit.domain.userdetail.skill.dto.SkillUpdateRequest;
import com.caro.bizkit.domain.userdetail.skill.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@Tag(name = "Skill", description = "스킬 정보 조회 API")
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    @Operation(summary = "스킬 목록 조회", description = "userId가 없으면 전체 스킬, 있으면 사용자 스킬을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "userId", required = false) Integer userId
    ) {
        if (userId == null) {
            List<SkillResponse> skills = skillService.getAllSkills();
            return ResponseEntity.ok(ApiResponse.success("스킬 목록 조회 성공", skills));
        }
        List<SkillResponse> skills = skillService.getSkillsByUserId(principal, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 스킬 조회 성공", skills));
    }

    @GetMapping("/me")
    @Operation(summary = "내 스킬 조회", description = "인증된 사용자의 스킬 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getMySkills(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<SkillResponse> skills = skillService.getMySkills(user);
        return ResponseEntity.ok(ApiResponse.success("내 스킬 조회 성공", skills));
    }

    @PutMapping("/me")
    @Operation(summary = "내 스킬 수정", description = "인증된 사용자의 스킬을 전체 교체합니다. 기존 스킬은 삭제되고 새로운 스킬 목록으로 대체됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<SkillResponse>>> updateMySkills(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody SkillUpdateRequest request
    ) {
        List<SkillResponse> skills = skillService.updateMySkills(user, request);
        return ResponseEntity.ok(ApiResponse.success("내 스킬 수정 성공", skills));
    }

    @DeleteMapping("/me/{skill_id}")
    @Operation(summary = "내 스킬 삭제", description = "인증된 사용자의 스킬을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMySkill(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("skill_id") Integer skillId
    ) {
        skillService.deleteMySkill(user, skillId);
        return ResponseEntity.ok(ApiResponse.success("내 스킬 삭제 성공", null));
    }
}
