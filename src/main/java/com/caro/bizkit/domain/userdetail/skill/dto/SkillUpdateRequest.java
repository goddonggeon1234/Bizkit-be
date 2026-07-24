package com.caro.bizkit.domain.userdetail.skill.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SkillUpdateRequest(
        @NotNull(message = "skillIds는 필수입니다")
        List<Integer> skillIds
) {
}
