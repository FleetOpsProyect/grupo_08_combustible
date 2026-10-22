package co.edu.univalle.fleetops.fuel.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Outbound port: assignments owned by Assignments (G2). */
public interface AssignmentPort {

    /**
     * Finds an assignment. Throws DependencyUnavailableException when the service does not answer in time
     * or the circuit is open.
     *
     * @param assignmentId id of the assignment
     * @return the assignment, or empty when it does not exist
     */
    Optional<AssignmentInfo> findById(UUID assignmentId);
}
