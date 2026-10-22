package co.edu.univalle.fleetops.fuel.application.usecase;

import co.edu.univalle.fleetops.fuel.application.exception.EventPublicationException;
import co.edu.univalle.fleetops.fuel.application.port.in.RelayOutboxUseCase;
import co.edu.univalle.fleetops.fuel.application.port.out.EventPublisherPort;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxMessage;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort;
import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Use case (Transactional Outbox, Polling Publisher): publishes the pending events.
 * If the broker fails, the event stays pending and is retried on the next run (at least once delivery;
 * consumers deduplicate by event_id).
 */
public class RelayOutboxService implements RelayOutboxUseCase {

    private final OutboxPort outboxPort;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;
    private final int batchSize;

    /**
     * Creates the use case.
     *
     * @param outboxPort     outbox port
     * @param eventPublisher broker port
     * @param clock          clock in the Colombia time zone
     * @param batchSize      maximum events per run
     */
    public RelayOutboxService(OutboxPort outboxPort, EventPublisherPort eventPublisher, Clock clock, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
        this.outboxPort = outboxPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Override
    public int relayPending() {
        int published = 0;
        for (OutboxMessage message : outboxPort.findPending(batchSize)) {
            try {
                eventPublisher.publish(message);
            } catch (EventPublicationException brokerDown) {
                return published;
            }
            outboxPort.markPublished(message.eventId(), OffsetDateTime.now(clock));
            published++;
        }
        return published;
    }
}
