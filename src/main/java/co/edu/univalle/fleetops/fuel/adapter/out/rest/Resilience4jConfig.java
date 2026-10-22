package co.edu.univalle.fleetops.fuel.adapter.out.rest;

import co.edu.univalle.fleetops.fuel.crosscutting.CorrelationIdPropagationInterceptor;
import co.edu.univalle.fleetops.fuel.utilities.AppConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the outbound REST adapters: one RestClient per dependency with connect and read timeout
 * (2 s, from configuration), the X-Correlation-Id propagation, and one Circuit Breaker per dependency.
 */
@Configuration
public class Resilience4jConfig {

    @Bean
    AssignmentRestClient assignmentRestClient(RestClient.Builder builder, AppConfig config,
                                              CorrelationIdPropagationInterceptor correlationId) {
        RestClient client = restClient(builder, config.assignments().baseUrl(), config, correlationId);
        return new AssignmentRestClient(client, circuitBreaker(AssignmentRestClient.DEPENDENCY, config),
                config.assignments().inProgressState());
    }

    @Bean
    RouteRestClient routeRestClient(RestClient.Builder builder, AppConfig config,
                                    CorrelationIdPropagationInterceptor correlationId) {
        RestClient client = restClient(builder, config.routes().baseUrl(), config, correlationId);
        return new RouteRestClient(client, circuitBreaker(RouteRestClient.DEPENDENCY, config),
                config.routes().inProgressStatus());
    }

    static CircuitBreaker circuitBreaker(String name, AppConfig config) {
        AppConfig.CircuitBreakerSettings settings = config.circuitBreaker();
        CircuitBreakerConfig circuitConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(settings.failureRateThreshold())
                .slidingWindowSize(settings.slidingWindowSize())
                .minimumNumberOfCalls(settings.minimumCalls())
                .waitDurationInOpenState(Duration.ofMillis(settings.waitOpenMs()))
                .build();
        return CircuitBreaker.of(name, circuitConfig);
    }

    private static RestClient restClient(RestClient.Builder builder, String baseUrl, AppConfig config,
                                         CorrelationIdPropagationInterceptor correlationId) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(config.http().timeoutMs());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(correlationId)
                .build();
    }
}
