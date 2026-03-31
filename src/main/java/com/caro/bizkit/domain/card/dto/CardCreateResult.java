package com.caro.bizkit.domain.card.dto;

public record CardCreateResult(
        CardResponse card,
        ResultType resultType
) {
    public enum ResultType {
        CREATED,
        DUPLICATE,
        CLAIMED
    }
}
