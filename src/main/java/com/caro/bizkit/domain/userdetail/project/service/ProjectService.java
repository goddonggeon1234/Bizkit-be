package com.caro.bizkit.domain.userdetail.project.service;

import com.caro.bizkit.common.aop.CardInfoUpdated;
import com.caro.bizkit.common.security.CardCollectionValidator;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.userdetail.project.dto.ProjectRequest;
import com.caro.bizkit.domain.userdetail.project.dto.ProjectResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.function.Consumer;
import com.caro.bizkit.domain.userdetail.project.entity.Project;
import com.caro.bizkit.domain.userdetail.project.repository.ProjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CardCollectionValidator cardCollectionValidator;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(UserPrincipal principal) {
        return projectRepository.findAllByUserIdAndDeletedAtIsNull(principal.id()).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByUserId(UserPrincipal principal, Integer userId) {
        cardCollectionValidator.validateAccess(principal.id(), userId);
        return projectRepository.findAllByUserIdAndDeletedAtIsNull(userId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @CardInfoUpdated
    @Transactional
    public ProjectResponse createMyProject(UserPrincipal principal, ProjectRequest request) {
        User user = userRepository.getReferenceById(principal.id());
        Project project = Project.create(
                user,
                request.name(),
                request.content(),
                request.start_date(),
                request.end_date()
        );
        return ProjectResponse.from(projectRepository.save(project));
    }

    @CardInfoUpdated
    @Transactional
    @PreAuthorize("@projectSecurity.isOwner(#projectId, authentication)")
    public ProjectResponse updateMyProject(
            UserPrincipal principal,
            Integer projectId,
            Map<String, Object> request
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (request == null) {
            return ProjectResponse.from(project);
        }

        applyUpdates(project, request);
        return ProjectResponse.from(project);
    }

    @CardInfoUpdated
    @Transactional
    @PreAuthorize("@projectSecurity.isOwner(#projectId, authentication)")
    public void deleteMyProject(UserPrincipal principal, Integer projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        projectRepository.delete(project);
    }

    private void applyUpdates(Project project, Map<String, Object> request) {
        applyIfPresent(request, "name", project::updateName);
        applyIfPresent(request, "content", project::updateContent);
        applyDateIfPresent(request, "start_date", project::updateStartDate);

        if (request.containsKey("is_progress")) {
            Boolean isProgress = (Boolean) request.get("is_progress");
            project.updateIsProgress(isProgress);
            if (Boolean.TRUE.equals(isProgress)) {
                project.updateEndDate(null);
            }
        }

        if (request.containsKey("end_date")) {
            Object value = request.get("end_date");
            if (value == null) {
                project.updateEndDate(null);
                project.updateIsProgress(Boolean.TRUE);
            } else {
                LocalDate endDate = value instanceof LocalDate ? (LocalDate) value : parseDate((String) value);
                project.updateEndDate(endDate);
                project.updateIsProgress(Boolean.FALSE);
            }
        }
    }

    private void applyIfPresent(Map<String, Object> request, String key, Consumer<String> updater) {
        if (request.containsKey(key)) {
            updater.accept((String) request.get(key));
        }
    }

    private void applyDateIfPresent(Map<String, Object> request, String key, Consumer<LocalDate> updater) {
        if (request.containsKey(key)) {
            Object value = request.get(key);
            if (value instanceof LocalDate) {
                updater.accept((LocalDate) value);
            } else if (value instanceof String) {
                updater.accept(parseDate((String) value));
            }
        }
    }

    private LocalDate parseDate(String value) {
        if (value.matches("\\d{4}-\\d{2}")) {
            return YearMonth.parse(value).atDay(1);
        }
        return LocalDate.parse(value);
    }
}
