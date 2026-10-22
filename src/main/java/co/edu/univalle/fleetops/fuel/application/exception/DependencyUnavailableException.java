package co.edu.univalle.fleetops.fuel.application.exception;

/** A service Fuel depends on did not answer in time, failed, or its circuit is open (503). */
public class DependencyUnavailableException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param dependency name of the service
     * @param cause      original error
     */
    public DependencyUnavailableException(String dependency, Throwable cause) {
        super(dependency + " is not available", cause);
    }
}
