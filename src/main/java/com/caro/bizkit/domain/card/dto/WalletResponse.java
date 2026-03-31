package com.caro.bizkit.domain.card.dto;

import com.caro.bizkit.domain.card.entity.Card;

public record WalletResponse(
        Integer id,
        Integer user_id,
        String uuid,
        String name,
        String email,
        String phone_number,
        String lined_number,
        String company,
        String position,
        String department,
        String ai_image_key
) {
    public static WalletResponse from(Card card) {
        return new WalletResponse(
                card.getId(),
                card.getUser() != null ? card.getUser().getId() : null,
                card.getUuid(),
                card.getName(),
                card.getEmail(),
                card.getPhoneNumber(),
                card.getLinedNumber(),
                card.getCompany(),
                card.getPosition(),
                card.getDepartment(),
                card.getAiImageKey()
        );
    }
}
