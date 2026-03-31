package com.caro.bizkit.observability;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FlowId {

    UNKNOWN("unknown", HttpMethod.ANY, List.of()),

    AUTH_LOGIN("AUTH_LOGIN", HttpMethod.POST, List.of("/api/auth/login")),
    AUTH_ROTATION("AUTH_ROTATION", HttpMethod.POST, List.of("/api/auth/rotation")),
    AUTH_LOGOUT("AUTH_LOGOUT", HttpMethod.POST, List.of("/api/auth/logout")),

    WALLET_LIST_GET("WALLET_LIST_GET", HttpMethod.GET, List.of("/api/wallets")),

    CARD_LIST_GET("CARD_LIST_GET", HttpMethod.GET, List.of("/api/cards", "/api/cards/me")),
    CARD_GET("CARD_GET", HttpMethod.GET, List.of("/api/cards/{card_id}", "/api/cards/me/latest", "/api/cards/uuid/{uuid}")),
    CARD_REGISTER("CARD_REGISTER", HttpMethod.POST, List.of("/api/wallets"));

    private final String flowId;
    private final HttpMethod method;
    private final List<String> pathTemplates;

    FlowId(String flowId, HttpMethod method, List<String> pathTemplates) {
        this.flowId = flowId;
        this.method = method;
        this.pathTemplates = pathTemplates;
    }

    public String flowId() {
        return flowId;
    }

    public HttpMethod method() {
        return method;
    }

    public List<String> pathTemplates() {
        return pathTemplates;
    }

    public String primaryTemplateOrUnknown() {
        return (pathTemplates == null || pathTemplates.isEmpty()) ? "UNKNOWN" : pathTemplates.getFirst();
    }

    private static final Map<String, FlowId> BY_FLOW_ID =
            Stream.of(values())
                    .collect(Collectors.toUnmodifiableMap(FlowId::flowId, Function.identity()));

    public static FlowId fromFlowId(String flowId) {
        if (flowId == null) return UNKNOWN;
        return BY_FLOW_ID.getOrDefault(flowId, UNKNOWN);
    }

    public enum HttpMethod {
        GET, POST, PUT, PATCH, DELETE, ANY
    }

    private record RouteKey(HttpMethod method, String template) {}

    private static final Map<RouteKey, FlowId> BY_ROUTE =
            Stream.of(values())
                    .flatMap(f -> f.pathTemplates().stream()
                            .map(t -> Map.entry(
                                    new RouteKey(f.method(), t), f)))
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));


    public static String resolve(String method, String template) {
        if (method == null || template == null)
            return UNKNOWN.flowId();

        HttpMethod incoming = toHttpMethod(method);

        FlowId flow = BY_ROUTE.get(new RouteKey(incoming, template));

        if (flow == null) {
            flow = BY_ROUTE.get(new RouteKey(HttpMethod.ANY, template));
        }

        return flow == null ? UNKNOWN.flowId() : flow.flowId();
    }

    private static HttpMethod toHttpMethod(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> HttpMethod.GET;
            case "POST" -> HttpMethod.POST;
            case "PUT" -> HttpMethod.PUT;
            case "PATCH" -> HttpMethod.PATCH;
            case "DELETE" -> HttpMethod.DELETE;
            default -> HttpMethod.ANY;
        };
    }

}