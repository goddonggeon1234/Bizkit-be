package com.caro.bizkit.common.S3.dto;

public enum UploadCategory {
    PROFILE("profile", null),
    QR("qr", null),
    AI("ai", null),
    OCR("ocr", 1),
    AI_CARD_TEMP("ai/card/temp", 1);

    private final String prefix;
    private final Integer lifetimeDays;

    UploadCategory(String prefix, Integer lifetimeDays) {
        this.prefix = prefix;
        this.lifetimeDays = lifetimeDays;
    }

    public String prefix() {
        return prefix;
    }

    public Integer lifetimeDays() {
        return lifetimeDays;
    }
}
