package com.caro.bizkit.domain.card.dto;

import com.caro.bizkit.domain.card.entity.Card;
import java.time.LocalDate;

public record CardResponse(
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
        LocalDate start_date,
        LocalDate end_date,
        Boolean is_progress,
        String ai_image_key,
        String description
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
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
                card.getStartDate(),
                card.getEndDate(),
                card.getIsProgress(),
                card.getAiImageKey(),
                card.getDescription()
        );
    }
}
