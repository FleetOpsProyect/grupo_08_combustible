package co.edu.univalle.fleetops.fuel.application.port.out;

import java.util.UUID;

/** What Fuel needs from an assignment of Assignments (G2), already translated to our terms. */
public record AssignmentInfo(UUID assignmentId, UUID vehicleId, UUID driverId, boolean inProgress) {
}
