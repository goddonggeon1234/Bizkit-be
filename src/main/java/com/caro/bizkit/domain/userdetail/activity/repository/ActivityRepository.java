package com.caro.bizkit.domain.userdetail.activity.repository;

import com.caro.bizkit.domain.userdetail.activity.entity.Activity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Integer> {
    List<Activity> findAllByUserId(Integer userId);
}
