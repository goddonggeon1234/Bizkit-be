package com.caro.bizkit.observability.redis;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;

public final class RedisExceptionClassifier {

    private RedisExceptionClassifier() {}

    public static String classify(Throwable t) {
        if (t == null) return "unknown";

        if (t instanceof QueryTimeoutException) return "timeout";
        if (t instanceof RedisConnectionFailureException) return "connection";

        String msg = t.getMessage();
        if (msg != null && msg.toUpperCase().contains("READONLY")) {
            return "readonly";
        }

        if (t instanceof RedisSystemException rse) {
            String m = rse.getMessage();
            if (m != null && m.toUpperCase().contains("READONLY")) {
                return "readonly";
            }
        }

        return "other";
    }
}