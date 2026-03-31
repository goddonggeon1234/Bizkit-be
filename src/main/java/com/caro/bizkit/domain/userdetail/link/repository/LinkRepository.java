package com.caro.bizkit.domain.userdetail.link.repository;

import com.caro.bizkit.domain.userdetail.link.entity.Link;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Integer> {
    List<Link> findAllByUserId(Integer userId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT l FROM Link l WHERE l.user.id = :userId AND l.link LIKE %:keyword%"
    )
    java.util.Optional<Link> findFirstByUserIdAndLinkContaining(
            @org.springframework.data.repository.query.Param("userId") Integer userId,
            @org.springframework.data.repository.query.Param("keyword") String keyword
    );
}
