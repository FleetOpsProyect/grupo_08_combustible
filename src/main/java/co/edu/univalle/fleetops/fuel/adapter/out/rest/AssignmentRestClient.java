package co.edu.univalle.fleetops.fuel.adapter.out.rest;

import co.edu.univalle.fleetops.fuel.application.exception.DependencyUnavailableException;
import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentInfo;
import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Outbound adapter (Adapter and Anti-Corruption Layer): calls GET /assignment/{id} of Assignments (G2)
 * with a 2 s timeout and a Circuit Breaker, and translates the answer to {@link AssignmentInfo}.
 * A 404 is a valid answer (the assignment does not exist) and does not count as a failure of the circuit.
 */
public class AssignmentRestClient implements AssignmentPort {

    static final String DEPENDENCY = "assignments";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final String inProgressState;

    /**
     * Creates the adapter.
     *
     * @param restClient      client with the base URL, timeouts and correlation id
     * @param circuitBreaker  circuit of this dependency
     * @param inProgressState value of "state" that means the assignment is in progress
     */
    public AssignmentRestClient(RestClient restClient, CircuitBreaker circuitBreaker, String inProgressState) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.inProgressState = inProgressState;
    }

    @Override
    public Optional<AssignmentInfo> findById(UUID assignmentId) {
        try {
            return circuitBreaker.executeSupplier(() -> fetch(assignmentId));
        } catch (CallNotPermittedException | RestClientException failure) {
            throw new DependencyUnavailableException(DEPENDENCY, failure);
        }
    }

    private Optional<AssignmentInfo> fetch(UUID assignmentId) {
        try {
            AssignmentResponse response = restClient.get()
                    .uri("/assignment/{id}", assignmentId)
                    .retrieve()
                    .body(AssignmentResponse.class);
            return Optional.ofNullable(response).map(body -> new AssignmentInfo(
                    assignmentId, body.vehicleId(), body.driverId(), inProgressState.equalsIgnoreCase(body.state())));
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
    }
}
