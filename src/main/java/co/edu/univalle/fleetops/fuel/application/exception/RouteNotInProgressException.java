package co.edu.univalle.fleetops.fuel.application.exception;

import java.util.UUID;

/** The assignment has no route in progress in Telemetry and Routes (409). */
public class RouteNotInProgressException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param assignmentId id of the assignment
     */
    public RouteNotInProgressException(UUID assignmentId) {
        super("Assignment has no route in progress: " + assignmentId);
    }
}
