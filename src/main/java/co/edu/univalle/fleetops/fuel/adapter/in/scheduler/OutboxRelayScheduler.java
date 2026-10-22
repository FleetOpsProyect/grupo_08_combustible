package co.edu.univalle.fleetops.fuel.adapter.in.scheduler;

import co.edu.univalle.fleetops.fuel.application.port.in.RelayOutboxUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter driven by a timer: every few seconds asks the core to publish the pending outbox events.
 * The interval comes from configuration (OUTBOX_RELAY_INTERVAL_MS).
 */
@Component
public class OutboxRelayScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final RelayOutboxUseCase relayOutbox;

    /**
     * Creates the scheduler.
     *
     * @param relayOutbox inbound port of the relay
     */
    public OutboxRelayScheduler(RelayOutboxUseCase relayOutbox) {
        this.relayOutbox = relayOutbox;
    }

    /**
     * Runs one relay cycle.
     *
     * @return number of events published in this cycle
     */
    @Scheduled(fixedDelayString = "${fuel.outbox.relay-interval-ms}")
    public int relay() {
        int published = relayOutbox.relayPending();
        if (published > 0) {
            LOG.info("Outbox relay published {} event(s)", published);
        }
        return published;
    }
}
