package com.caro.bizkit.domain.userdetail.skill.repository;

import com.caro.bizkit.domain.userdetail.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Integer> {
}
