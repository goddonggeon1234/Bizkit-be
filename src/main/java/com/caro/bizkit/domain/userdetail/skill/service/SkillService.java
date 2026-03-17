package com.caro.bizkit.domain.userdetail.skill.service;

import com.caro.bizkit.common.security.CardCollectionValidator;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.userdetail.skill.dto.SkillResponse;
import com.caro.bizkit.domain.userdetail.skill.dto.SkillUpdateRequest;
import com.caro.bizkit.domain.userdetail.skill.entity.Skill;
import com.caro.bizkit.domain.userdetail.skill.repository.SkillRepository;
import com.caro.bizkit.domain.userdetail.skill.repository.UserSkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final CardCollectionValidator cardCollectionValidator;

    @Cacheable(cacheNames = "skills", cacheManager = "caffeineCacheManager")
    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(SkillResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getMySkills(UserPrincipal principal) {
        return userSkillRepository.findAllByUserId(principal.id()).stream()
                .map(userSkill -> SkillResponse.from(userSkill.getSkill()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkillsByUserId(UserPrincipal principal, Integer userId) {
        cardCollectionValidator.validateAccess(principal.id(), userId);
        return userSkillRepository.findAllByUserId(userId).stream()
                .map(userSkill -> SkillResponse.from(userSkill.getSkill()))
                .toList();
    }

    @Transactional
    public void deleteMySkill(UserPrincipal principal, Integer skillId) {
        var userSkill = userSkillRepository.findByUserIdAndSkillId(principal.id(), skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User skill not found"));
        userSkillRepository.delete(userSkill);
    }

    @Transactional
    public List<SkillResponse> updateMySkills(UserPrincipal principal, SkillUpdateRequest request) {
        userSkillRepository.deleteAllByUserId(principal.id());

        if (request.skillIds() == null || request.skillIds().isEmpty()) {
            return List.of();
        }

        List<Skill> skills = skillRepository.findAllById(request.skillIds());

        if (skills.size() != request.skillIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 스킬 ID가 포함되어 있습니다");
        }

        userSkillRepository.insertAllByUserIdAndSkillIds(principal.id(), request.skillIds());

        return skills.stream()
                .map(SkillResponse::from)
                .toList();
    }
}
