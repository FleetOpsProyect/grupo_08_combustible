package co.edu.univalle.fleetops.fuel.application.exception;

import java.util.UUID;

/** The assignment does not exist in Assignments (404). */
public class AssignmentNotFoundException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param assignmentId id that was looked up
     */
    public AssignmentNotFoundException(UUID assignmentId) {
        super("Assignment not found: " + assignmentId);
    }
}
