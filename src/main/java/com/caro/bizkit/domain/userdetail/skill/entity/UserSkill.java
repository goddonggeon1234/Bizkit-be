package com.caro.bizkit.domain.userdetail.skill.entity;


import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_skill")
public class UserSkill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    public UserSkill(User user, Skill skill) {
        this.user = user;
        this.skill = skill;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
