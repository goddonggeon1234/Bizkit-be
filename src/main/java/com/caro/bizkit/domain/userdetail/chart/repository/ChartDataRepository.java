package com.caro.bizkit.domain.userdetail.chart.repository;

import com.caro.bizkit.domain.userdetail.chart.entity.ChartData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChartDataRepository extends JpaRepository<ChartData, Integer> {
    List<ChartData> findAllByUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM ChartData c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);
}
