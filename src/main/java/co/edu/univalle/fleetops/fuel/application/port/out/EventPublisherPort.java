package co.edu.univalle.fleetops.fuel.application.port.out;

/** Outbound port: event broker. The broker technology is not decided yet; only the adapter changes. */
public interface EventPublisherPort {

    /**
     * Publishes one event. Throws EventPublicationException when the broker is not available.
     *
     * @param message event taken from the outbox
     */
    void publish(OutboxMessage message);
}
