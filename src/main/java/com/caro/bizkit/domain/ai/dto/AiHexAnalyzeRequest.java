package com.caro.bizkit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiHexAnalyzeRequest(
        @JsonProperty("user_id") Integer userId,
        @JsonProperty("github_username") String githubUsername,
        Capabilities capabilities,
        Reviews reviews
) {
    public record Capabilities(
            List<Career> career,
            List<String> skills,
            List<Project> projects,
            List<Achievement> achievements
    ) {}

    public record Career(
            @JsonProperty("company_name") String companyName,
            String department,
            String position,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate
    ) {}

    public record Project(
            @JsonProperty("project_name") String projectName,
            String description,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate
    ) {}

    public record Achievement(
            String title,
            String grade,
            String organization,
            String description,
            @JsonProperty("award_date") String awardDate
    ) {}

    public record Reviews(
            @JsonProperty("text_reviews") List<String> textReviews,
            @JsonProperty("badge_reviews") BadgeReviews badgeReviews
    ) {}

    public record BadgeReviews(
            Double collaboration,
            Double communication,
            Double technical,
            Double documentation,
            Double reliability,
            Double preference
    ) {}
}
