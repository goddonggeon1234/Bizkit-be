package com.caro.bizkit.domain.userdetail.activity.dto;

import com.caro.bizkit.domain.userdetail.activity.entity.Activity;
import java.time.LocalDate;

public record ActivityResponse(
        Integer id,
        String name,
        String grade,
        String organization,
        String content,
        LocalDate winDate
) {
    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getName(),
                activity.getGrade(),
                activity.getOrganization(),
                activity.getContent(),
                activity.getWinDate()
        );
    }
}
