package com.caro.bizkit.domain.ai.dto;

import com.caro.bizkit.domain.ai.entity.CardStyleTag;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record AiCardGenerationRequest(
        @NotNull @JsonProperty("card_id") Integer cardId,
        @NotNull CardStyleTag tag,
        String text
) {}
