package com.caro.bizkit.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class CustomException extends RuntimeException {

    private final HttpStatusCode status;

    public CustomException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }
}
