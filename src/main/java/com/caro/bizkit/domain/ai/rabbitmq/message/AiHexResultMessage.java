package com.caro.bizkit.domain.ai.rabbitmq.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiHexResultMessage(
        @JsonProperty("backend_task_id") Integer backendTaskId,
        @JsonProperty("ai_task_id") String aiTaskId,
        @JsonProperty("user_id") Integer userId,
        String status,
        Data data,
        String error
) {
    public record Data(
            @JsonProperty("radar_chart") RadarChart radarChart,
            @JsonProperty("confidence_level") String confidenceLevel,
            @JsonProperty("analysis_summary") AnalysisSummary analysisSummary
    ) {}

    public record RadarChart(
            Integer collaboration,
            Integer communication,
            Integer technical,
            Integer documentation,
            Integer reliability,
            Integer preference
    ) {}

    public record AnalysisSummary(
            String collaboration,
            String communication,
            String technical,
            String documentation,
            String reliability,
            String preference
    ) {}
}
