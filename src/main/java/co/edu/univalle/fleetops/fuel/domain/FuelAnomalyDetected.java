package co.edu.univalle.fleetops.fuel.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event: a fuel record was marked as anomalous consumption.
 * The outbound adapter translates it to the contract event (fuel_anomaly_detected_event).
 */
public record FuelAnomalyDetected(
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

    /**
     * Builds the event from an anomalous fuel record.
     *
     * @param fuelRecord the anomalous fuel record
     * @param average    vehicle average it was compared with
     * @return the domain event
     */
    public static FuelAnomalyDetected of(FuelRecord fuelRecord, VehicleFuelAverage average) {
        return new FuelAnomalyDetected(
                fuelRecord.id(),
                fuelRecord.assignmentId(),
                fuelRecord.vehicleId(),
                fuelRecord.driverId(),
                fuelRecord.routeId(),
                fuelRecord.efficiencyKmPerGallon(),
                average.averageKmPerGallon(),
                fuelRecord.gallonsPurchased(),
                fuelRecord.totalCost(),
                fuelRecord.recordedAt());
    }
}
