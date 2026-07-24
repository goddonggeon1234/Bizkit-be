package com.caro.bizkit.domain.userdetail.project.repository;

import com.caro.bizkit.domain.userdetail.project.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findAllByUserId(Integer userId);
    Optional<Project> findByIdAndUserId(Integer id, Integer userId);
}
