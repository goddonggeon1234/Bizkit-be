package com.caro.bizkit.domain.review.dto.response;

import com.caro.bizkit.domain.review.entity.Tag;

public record TagResponse(
        Integer id,
        String keyword
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getKeyword());
    }
}
