package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import co.edu.univalle.fleetops.fuel.domain.FuelAnomalyDetected;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contract of fuel_anomaly_detected_event: standard envelope of the shared conventions
 * (event_id, event_type, occurred_at in GMT-5, source, data), in English and snake_case. The version
 * field was dropped by the groups on 26 Oct 2026.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record FuelAnomalyDetectedMessage(
        UUID eventId,
        String eventType,
        OffsetDateTime occurredAt,
        String source,
        Data data) {

    static final String EVENT_TYPE = "fuel_anomaly_detected_event";
    /** Slug of the service, used as "source" and in the name of its SNS topic (fuel_events). */
    static final String SERVICE_SLUG = "fuel";

    static FuelAnomalyDetectedMessage of(UUID eventId, OffsetDateTime occurredAt, FuelAnomalyDetected event) {
        return new FuelAnomalyDetectedMessage(eventId, EVENT_TYPE, occurredAt, SERVICE_SLUG, new Data(
                event.fuelRecordId(), event.assignmentId(), event.vehicleId(), event.driverId(), event.routeId(),
                event.efficiencyKmPerGallon(), event.averageEfficiencyKmPerGallon(), event.gallonsPurchased(),
                event.totalCost(), event.recordedAt()));
    }

    /** Payload ("data") of the event. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Data(
            UUID fuelRecordId,
            UUID assignmentId,
            UUID vehicleId,
            UUID driverId,
            UUID routeId,
            BigDecimal efficiencyKmPerGallon,
            BigDecimal averageEfficiencyKmPerGallon,
            BigDecimal gallonsPurchased,
            BigDecimal totalCost,
            OffsetDateTime recordedAt) {
    }
}
