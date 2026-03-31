package com.caro.bizkit.domain.userdetail.project.dto;

import com.caro.bizkit.domain.userdetail.project.entity.Project;
import java.time.LocalDate;

public record ProjectResponse(
        Integer id,
        String name,
        String content,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isProgress
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getContent(),
                project.getStartDate(),
                project.getEndDate(),
                project.getIsProgress()
        );
    }
}
