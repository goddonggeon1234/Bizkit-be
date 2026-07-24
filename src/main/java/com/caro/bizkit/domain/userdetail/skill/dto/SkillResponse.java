package com.caro.bizkit.domain.userdetail.skill.dto;

import com.caro.bizkit.domain.userdetail.skill.entity.Skill;

public record SkillResponse(
        Integer id,
        String name
) {
    public static SkillResponse from(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName()
        );
    }
}
