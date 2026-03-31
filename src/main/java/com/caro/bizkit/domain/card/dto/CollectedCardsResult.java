package com.caro.bizkit.domain.card.dto;

import java.util.List;

public record CollectedCardsResult(
        List<WalletResponse> data,
        Integer cursorId,
        boolean hasNext
) {
}
