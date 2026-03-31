package com.caro.bizkit.observability.auth;

public enum AuthRotationFailureReason {
    TOKEN_NOT_FOUND,
    HASH_MISMATCH,
    REDIS_TIMEOUT,
    REDIS_CONNECTION,
    REDIS_READONLY,
    INVALID_REFRESH,
    UNEXPECTED;

    public String tag() {
        return name().toLowerCase();
    }
}