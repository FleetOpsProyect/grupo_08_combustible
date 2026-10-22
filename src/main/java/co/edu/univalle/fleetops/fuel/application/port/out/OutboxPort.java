package co.edu.univalle.fleetops.fuel.application.port.out;

import co.edu.univalle.fleetops.fuel.domain.FuelAnomalyDetected;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Outbound port (Transactional Outbox): events stored in the same transaction as the fuel record. */
public interface OutboxPort {

    /**
     * Stores the event so it is published later.
     *
     * @param event      domain event
     * @param occurredAt moment the event happened
     */
    void save(FuelAnomalyDetected event, OffsetDateTime occurredAt);

    /**
     * Pending events, oldest first.
     *
     * @param limit maximum number of events
     * @return pending events
     */
    List<OutboxMessage> findPending(int limit);

    void markPublished(UUID eventId, OffsetDateTime publishedAt);
}
