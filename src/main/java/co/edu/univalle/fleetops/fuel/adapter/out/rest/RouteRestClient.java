package co.edu.univalle.fleetops.fuel.adapter.out.rest;

import co.edu.univalle.fleetops.fuel.application.exception.DependencyUnavailableException;
import co.edu.univalle.fleetops.fuel.application.port.out.RoutePort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Outbound adapter (Adapter and Anti-Corruption Layer): asks Telemetry and Routes (G6) for the route in
 * progress of an assignment, with a 2 s timeout and a Circuit Breaker.
 */
public class RouteRestClient implements RoutePort {

    static final String DEPENDENCY = "routes";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final String inProgressStatus;

    /**
     * Creates the adapter.
     *
     * @param restClient       client with the base URL, timeouts and correlation id
     * @param circuitBreaker   circuit of this dependency
     * @param inProgressStatus value of the status filter for a route in progress
     */
    public RouteRestClient(RestClient restClient, CircuitBreaker circuitBreaker, String inProgressStatus) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.inProgressStatus = inProgressStatus;
    }

    @Override
    public Optional<UUID> findRouteInProgress(UUID assignmentId) {
        try {
            return circuitBreaker.executeSupplier(() -> fetch(assignmentId));
        } catch (CallNotPermittedException | RestClientException failure) {
            throw new DependencyUnavailableException(DEPENDENCY, failure);
        }
    }

    private Optional<UUID> fetch(UUID assignmentId) {
        RoutePageResponse page = restClient.get()
                .uri(builder -> builder.path("/routes")
                        .queryParam("assignment_id", assignmentId)
                        .queryParam("status", inProgressStatus)
                        .build())
                .retrieve()
                .body(RoutePageResponse.class);
        return Optional.ofNullable(page)
                .map(RoutePageResponse::content)
                .flatMap(routes -> routes.stream().findFirst())
                .map(RoutePageResponse.Route::id);
    }
}
