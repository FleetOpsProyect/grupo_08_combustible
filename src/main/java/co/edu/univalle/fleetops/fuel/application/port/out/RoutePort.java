package co.edu.univalle.fleetops.fuel.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Outbound port: routes owned by Telemetry and Routes (G6). */
public interface RoutePort {

    /**
     * Finds the route in progress of an assignment. Throws DependencyUnavailableException when the service
     * does not answer in time or the circuit is open.
     *
     * @param assignmentId id of the assignment
     * @return the route id, or empty when the assignment has no route in progress
     */
    Optional<UUID> findRouteInProgress(UUID assignmentId);
}
