package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** JPA entity of the outbox_events table (Transactional Outbox). */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected OutboxEventEntity() {
        // Required by JPA
    }

    OutboxEventEntity(UUID eventId, String eventType, UUID aggregateId, String payload, OffsetDateTime occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    void markPublished(OffsetDateTime when) {
        this.publishedAt = when;
    }

    UUID getEventId() {
        return eventId;
    }

    String getEventType() {
        return eventType;
    }

    String getPayload() {
        return payload;
    }

    OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
