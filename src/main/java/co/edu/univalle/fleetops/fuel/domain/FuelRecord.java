package co.edu.univalle.fleetops.fuel.domain;

import co.edu.univalle.fleetops.fuel.domain.exception.InvalidFuelRecordException;
import co.edu.univalle.fleetops.fuel.domain.exception.OdometerNotIncreasingException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain entity: one refueling made by a driver during a route in progress.
 * Pure Java (hexagonal core): no framework annotations. The invariants are checked on creation.
 *
 * @param efficiencyKmPerGallon null when the tank was not filled or there is no previous full tank
 */
public record FuelRecord(
        UUID id,
        UUID assignmentId,
        UUID vehicleId,
        UUID driverId,
        UUID routeId,
        BigDecimal gallonsPurchased,
        BigDecimal totalCost,
        BigDecimal odometerKm,
        boolean fullTank,
        BigDecimal efficiencyKmPerGallon,
        boolean anomalousConsumption,
        OffsetDateTime recordedAt) {

    public FuelRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(assignmentId, "assignmentId");
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(driverId, "driverId");
        Objects.requireNonNull(routeId, "routeId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        requirePositive(gallonsPurchased, "gallons_purchased");
        requirePositive(totalCost, "total_cost");
        requirePositive(odometerKm, "odometer_km");
        if (anomalousConsumption && efficiencyKmPerGallon == null) {
            throw new InvalidFuelRecordException("efficiency_km_per_gallon");
        }
    }

    /**
     * Business rule: the odometer reading must be greater than the last reading of the vehicle.
     *
     * @param previousOdometerKm last odometer reading of the vehicle, if any
     * @param currentOdometerKm  odometer reading of the new refueling
     */
    public static void ensureOdometerIncreases(Optional<BigDecimal> previousOdometerKm,
                                               BigDecimal currentOdometerKm) {
        previousOdometerKm
                .filter(previous -> currentOdometerKm.compareTo(previous) <= 0)
                .ifPresent(previous -> {
                    throw new OdometerNotIncreasingException(previous, currentOdometerKm);
                });
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new InvalidFuelRecordException(field);
        }
    }
}
