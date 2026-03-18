package com.caro.bizkit.domain.ai.entity;

public interface AiTask {
    void fail();
    AiAnalysisStatus getStatus();
    Integer getUserId();
}
