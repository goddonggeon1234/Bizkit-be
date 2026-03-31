package com.caro.bizkit.domain.userdetail.project.controller;

import com.caro.bizkit.common.ApiResponse.ApiResponse;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.userdetail.project.dto.ProjectRequest;
import com.caro.bizkit.domain.userdetail.project.dto.ProjectResponse;
import java.util.Map;
import com.caro.bizkit.domain.userdetail.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project", description = "프로젝트 정보 조회 API")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/me")
    @Operation(summary = "내 프로젝트 조회", description = "인증된 사용자의 프로젝트 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<ProjectResponse> projects = projectService.getMyProjects(user);
        return ResponseEntity.ok(ApiResponse.success("내 프로젝트 조회 성공", projects));
    }

    @GetMapping
    @Operation(summary = "사용자 프로젝트 조회", description = "지정된 사용자 프로젝트 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectsByUserId(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("userId") Integer userId
    ) {
        List<ProjectResponse> projects = projectService.getProjectsByUserId(principal, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 프로젝트 조회 성공", projects));
    }

    @PostMapping("/me")
    @Operation(summary = "내 프로젝트 생성", description = "인증된 사용자의 프로젝트를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")

    })
    public ResponseEntity<ApiResponse<ProjectResponse>> createMyProject(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ProjectRequest request
    ) {
        ProjectResponse project = projectService.createMyProject(user, request);
        return ResponseEntity.ok(ApiResponse.success("내 프로젝트 생성 성공", project));
    }

    @PatchMapping("/{project_id}")
    @Operation(summary = "프로젝트 수정", description = "프로젝트 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("project_id") Integer projectId,
            @RequestBody Map<String, Object> request
    ) {
        ProjectResponse project = projectService.updateMyProject(user, projectId, request);
        return ResponseEntity.ok(ApiResponse.success("프로젝트 수정 성공", project));
    }

    @DeleteMapping("/{project_id}")
    @Operation(summary = "프로젝트 삭제", description = "프로젝트 정보를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable("project_id") Integer projectId
    ) {
        projectService.deleteMyProject(user, projectId);
        return ResponseEntity.ok(ApiResponse.success("프로젝트 삭제 성공", null));
    }
}
