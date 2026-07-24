package com.caro.bizkit.domain.review.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReviewCreateRequest(
        @NotNull
        @JsonProperty("reviewee_id")
        Integer revieweeId,

        @NotNull
        @Size(min = 1, max = 3)
        @JsonProperty("tag_id_list")
        List<Integer> tagIdList,

        String comment,

        @NotNull
        @Min(1) @Max(5)
        Integer score
) {}
