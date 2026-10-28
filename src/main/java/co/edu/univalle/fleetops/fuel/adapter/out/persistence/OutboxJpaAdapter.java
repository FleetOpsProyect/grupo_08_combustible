package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import co.edu.univalle.fleetops.fuel.application.port.out.OutboxMessage;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort;
import co.edu.univalle.fleetops.fuel.crosscutting.CorrelationIdFilter;
import co.edu.univalle.fleetops.fuel.domain.FuelAnomalyDetected;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter (Transactional Outbox): stores the contract message in outbox_events, inside the same
 * transaction that stores the fuel record, and lets the relay read and mark it.
 */
@Component
public class OutboxJpaAdapter implements OutboxPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Supplier<UUID> idGenerator;

    /**
     * Creates the adapter.
     *
     * @param repository   Spring Data repository
     * @param objectMapper JSON serializer of the contract message
     * @param idGenerator  generator of event ids
     */
    public OutboxJpaAdapter(OutboxEventJpaRepository repository, ObjectMapper objectMapper,
                            Supplier<UUID> idGenerator) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void save(FuelAnomalyDetected event, OffsetDateTime occurredAt) {
        UUID eventId = idGenerator.get();
        FuelAnomalyDetectedMessage message = FuelAnomalyDetectedMessage.of(eventId, occurredAt, correlationId(),
                event);
        repository.save(new OutboxEventEntity(eventId, FuelAnomalyDetectedMessage.EVENT_TYPE,
                event.fuelRecordId(), toJson(message), occurredAt));
    }

    @Override
    public List<OutboxMessage> findPending(int limit) {
        return repository.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, limit)).stream()
                .map(entity -> new OutboxMessage(entity.getEventId(), entity.getEventType(), entity.getPayload()))
                .toList();
    }

    @Override
    public void markPublished(UUID eventId, OffsetDateTime publishedAt) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.markPublished(publishedAt);
            repository.save(entity);
        });
    }

    /** Correlation id of the request that created the event; a new one when there is no request (convention). */
    private String correlationId() {
        String current = MDC.get(CorrelationIdFilter.MDC_KEY);
        return current == null || current.isBlank() ? idGenerator.get().toString() : current;
    }

    private String toJson(FuelAnomalyDetectedMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException serializationError) {
            throw new IllegalStateException("Could not serialize " + message.eventType(), serializationError);
        }
    }
}
