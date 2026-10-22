package co.edu.univalle.fleetops.fuel.domain.exception;

/** A fuel record breaks one of its invariants (for example, non positive gallons). */
public class InvalidFuelRecordException extends RuntimeException {

    private final String field;

    /**
     * Creates the exception.
     *
     * @param field contract name of the invalid field
     */
    public InvalidFuelRecordException(String field) {
        super("Invalid value for " + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
