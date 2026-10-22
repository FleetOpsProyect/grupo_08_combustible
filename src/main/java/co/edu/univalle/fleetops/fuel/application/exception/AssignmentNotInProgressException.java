package co.edu.univalle.fleetops.fuel.application.exception;

import java.util.UUID;

/** The assignment exists but is not in progress (409). */
public class AssignmentNotInProgressException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param assignmentId id of the assignment
     */
    public AssignmentNotInProgressException(UUID assignmentId) {
        super("Assignment is not in progress: " + assignmentId);
    }
}
