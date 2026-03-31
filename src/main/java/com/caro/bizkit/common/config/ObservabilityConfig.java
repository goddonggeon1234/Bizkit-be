package com.caro.bizkit.common.config;

import com.caro.bizkit.observability.FlowId;
import com.caro.bizkit.observability.FlowTaggingObservationConvention;
import com.caro.bizkit.observability.HttpMetricsFilterProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(HttpMetricsFilterProperties.class)
class ObservabilityConfig {

    private final HttpMetricsFilterProperties props;

    @Bean
    ServerRequestObservationConvention serverRequestObservationConvention() {
        return new FlowTaggingObservationConvention();
    }

    @Bean
    @Order(0)
    MeterFilter denyNonBusinessHttpRequests() {
        AntPathMatcher matcher = new AntPathMatcher();

        return MeterFilter.deny(id -> {
            if (!id.getName().startsWith("http.server.requests")) return false;

            String uri = id.getTag("uri");
            if (uri == null) return false;

            if (props.isDenyUnknownUri() && "UNKNOWN".equals(uri)) return true;

            if (props.getDenyUriExact().contains(uri)) return true;

            for (String pattern : props.getDenyUriPattern()) {
                if (matcher.match(pattern, uri)) {
                    if ("/actuator/prometheus".equals(uri)) return false;
                    return true;
                }
            }
            return false;
        });
    }


    @Bean
    MeterFilter normalizeHttpServerRequestsUriByFlow() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (!"http.server.requests".equals(id.getName())) {
                    return id;
                }

                String flow = id.getTag("flow");
                if (flow == null || flow.isBlank() || "unknown".equals(flow)) {
                    return id;
                }

                FlowId flowId = FlowId.fromFlowId(flow);
                if (flowId == FlowId.UNKNOWN) {
                    return id;
                }

                String normalizedUri = flowId.primaryTemplateOrUnknown();

                return id.replaceTags(
                        Tags.concat(
                                id.getTags().stream().filter(t -> !t.getKey().equals("uri")).toList(),
                                List.of(Tag.of("uri", normalizedUri))
                        )
                );
            }
        };
    }
}