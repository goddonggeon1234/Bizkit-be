package com.caro.bizkit.domain.review.repository;

import com.caro.bizkit.domain.review.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Integer> {
}
