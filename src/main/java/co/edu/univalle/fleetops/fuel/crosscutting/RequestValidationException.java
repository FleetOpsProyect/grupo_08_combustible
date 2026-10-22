package co.edu.univalle.fleetops.fuel.crosscutting;

import java.util.List;

/** One or more request parameters are invalid (400). */
public class RequestValidationException extends RuntimeException {

    private final transient List<FieldViolation> violations;

    /**
     * Creates the exception.
     *
     * @param violations every invalid field, not only the first one
     */
    public RequestValidationException(List<FieldViolation> violations) {
        super("Invalid request parameters");
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> violations() {
        return violations;
    }
}
