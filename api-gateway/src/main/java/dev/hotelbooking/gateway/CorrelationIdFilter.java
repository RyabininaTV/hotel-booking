package dev.hotelbooking.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String HEADER = "X-Correlation-Id";

    @Override
    public reactor.core.publisher.Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                                                     org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming;
        var request = exchange.getRequest().mutate().header(HEADER, correlationId).build();
        var response = exchange.getResponse();
        response.getHeaders().set(HEADER, correlationId);
        log.info("gateway_request correlationId={} method={} path={}", correlationId,
                request.getMethod(), request.getURI().getPath());
        return chain.filter(exchange.mutate().request(request).build())
                .doOnSuccess(ignored -> log.info("gateway_response correlationId={} status={}",
                        correlationId, response.getStatusCode()))
                .doOnError(error -> log.warn("gateway_error correlationId={} reason={}", correlationId, error.getMessage()));
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
