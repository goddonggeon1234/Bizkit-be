package com.caro.bizkit.domain.userdetail.link.dto;

import com.caro.bizkit.domain.userdetail.link.entity.Link;

public record LinkResponse(
        Integer id,
        String title,
        String link
) {
    public static LinkResponse from(Link linkEntity) {
        return new LinkResponse(
                linkEntity.getId(),
                linkEntity.getTitle(),
                linkEntity.getLink()
        );
    }
}
