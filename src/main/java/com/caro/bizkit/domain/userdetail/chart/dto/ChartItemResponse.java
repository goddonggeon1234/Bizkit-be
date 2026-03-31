package com.caro.bizkit.domain.userdetail.chart.dto;

import com.caro.bizkit.domain.userdetail.chart.entity.ChartData;

public record ChartItemResponse(
        String name,
        Integer value,
        String description
) {
    private static final java.util.Map<String, String> NAME_MAP = java.util.Map.of(
            "collaboration", "협업 능력",
            "communication", "소통 능력",
            "technical", "기술 역량",
            "documentation", "문서화 능력",
            "reliability", "신뢰도",
            "preference", "선호도"
    );

    public static ChartItemResponse from(ChartData chartData) {
        return new ChartItemResponse(
                NAME_MAP.getOrDefault(chartData.getName(), chartData.getName()),
                chartData.getValue(),
                chartData.getDescription()
        );
    }
}
