package co.edu.univalle.fleetops.fuel.application.exception;

/** The event broker could not receive an event; the event stays in the outbox. */
public class EventPublicationException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what failed
     * @param cause   original error
     */
    public EventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
