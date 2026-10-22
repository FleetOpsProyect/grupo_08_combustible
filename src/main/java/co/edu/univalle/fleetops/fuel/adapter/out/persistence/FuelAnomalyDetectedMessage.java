package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import co.edu.univalle.fleetops.fuel.domain.FuelAnomalyDetected;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contract of fuel_anomaly_detected_event: standard envelope of the shared conventions
 * (event_id, event_type, version, occurred_at in GMT-5, source, data), in English and snake_case.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record FuelAnomalyDetectedMessage(
        UUID eventId,
        String eventType,
        String version,
        OffsetDateTime occurredAt,
        String source,
        Data data) {

    static final String EVENT_TYPE = "fuel_anomaly_detected_event";
    static final String VERSION = "1.0";
    static final String SOURCE = "fuel";

    static FuelAnomalyDetectedMessage of(UUID eventId, OffsetDateTime occurredAt, FuelAnomalyDetected event) {
        return new FuelAnomalyDetectedMessage(eventId, EVENT_TYPE, VERSION, occurredAt, SOURCE, new Data(
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
