package com.caro.bizkit.domain.userdetail.chart.entity;


import com.caro.bizkit.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chart_data")
public class ChartData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 10, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer value; // TINYINT(0-100) -> Integer

    @Column(columnDefinition = "TEXT")
    private String description;

    public static ChartData create(User user, String name, Integer value, String description) {
        ChartData chartData = new ChartData();
        chartData.user = user;
        chartData.name = name;
        chartData.value = value;
        chartData.description = description;
        return chartData;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
