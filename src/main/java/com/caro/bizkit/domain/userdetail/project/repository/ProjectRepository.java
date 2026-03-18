package com.caro.bizkit.domain.userdetail.project.repository;

import com.caro.bizkit.domain.userdetail.project.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findAllByUserIdAndDeletedAtIsNull(Integer userId);
    Optional<Project> findByIdAndUserIdAndDeletedAtIsNull(Integer id, Integer userId);
}
