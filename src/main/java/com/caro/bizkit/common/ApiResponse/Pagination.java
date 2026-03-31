package com.caro.bizkit.common.ApiResponse;

public record Pagination(
        Integer cursorId,
        boolean has_next
) {
}
