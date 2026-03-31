package com.caro.bizkit.observability;

import io.micrometer.common.KeyValues;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.servlet.HandlerMapping;

public class FlowTaggingObservationConvention extends DefaultServerRequestObservationConvention {
    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        KeyValues base = super.getLowCardinalityKeyValues(context);

        HttpServletRequest request = extractRequest(context);

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String path = extractUriTemplateOrPath(request);

        String flow = FlowId.resolve(method, path);

        return base.and("flow", flow);
    }

    private HttpServletRequest extractRequest(ServerRequestObservationContext context) {
        Object carrier = context.getCarrier();
        return (carrier instanceof HttpServletRequest req) ? req : null;
    }

    private String extractUriTemplateOrPath(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";

        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) return pattern;

        return request.getRequestURI();
    }

}
