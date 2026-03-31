package com.caro.bizkit.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "metrics.http")
@Getter
@Setter
public class HttpMetricsFilterProperties {

    private List<String> denyUriExact = List.of();
    private List<String> denyUriPattern = List.of();
    private boolean denyUnknownUri = true;

    public List<String> getDenyUriExact() {
        return denyUriExact.stream().map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    public List<String> getDenyUriPattern() {
        return denyUriPattern.stream().map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}