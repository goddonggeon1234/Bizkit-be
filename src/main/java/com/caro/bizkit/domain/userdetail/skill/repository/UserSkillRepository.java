package com.caro.bizkit.domain.userdetail.skill.repository;

import com.caro.bizkit.domain.userdetail.skill.entity.UserSkill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSkillRepository extends JpaRepository<UserSkill, Integer> {
    @Query("select us from UserSkill us join fetch us.skill where us.user.id = :userId")
    List<UserSkill> findAllByUserId(@Param("userId") Integer userId);

    Optional<UserSkill> findByUserIdAndSkillId(Integer userId, Integer skillId);

    @Modifying
    @Query("DELETE FROM UserSkill us WHERE us.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query(value = "INSERT INTO user_skill (user_id, skill_id) SELECT :userId, id FROM skill WHERE id IN :skillIds",
           nativeQuery = true)
    void insertAllByUserIdAndSkillIds(@Param("userId") Integer userId, @Param("skillIds") List<Integer> skillIds);
}
