package co.edu.univalle.fleetops.fuel.application.port.in;

/** Inbound port (driven by a timer): publish the pending events of the outbox. */
public interface RelayOutboxUseCase {

    /**
     * Publishes the pending events, oldest first. Stops at the first failure so the order is kept.
     *
     * @return number of events published
     */
    int relayPending();
}
