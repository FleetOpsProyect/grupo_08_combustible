package co.edu.univalle.fleetops.fuel.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity: running average of the non anomalous efficiencies of a vehicle.
 * Kept as sum and count so it is updated without recalculating the whole history.
 */
public record VehicleFuelAverage(UUID vehicleId, BigDecimal efficiencySum, int sampleCount) {

    private static final int SCALE = 2;

    public VehicleFuelAverage {
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(efficiencySum, "efficiencySum");
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount must not be negative");
        }
    }

    /**
     * Average of a vehicle without history.
     *
     * @param vehicleId vehicle
     * @return an average with no samples
     */
    public static VehicleFuelAverage empty(UUID vehicleId) {
        return new VehicleFuelAverage(vehicleId, BigDecimal.ZERO, 0);
    }

    /**
     * Current average in km per gallon.
     *
     * @return the average, or zero when there are no samples
     */
    public BigDecimal averageKmPerGallon() {
        if (sampleCount == 0) {
            return BigDecimal.ZERO;
        }
        return efficiencySum.divide(BigDecimal.valueOf(sampleCount), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Adds a non anomalous efficiency (anomalous ones never enter the average).
     *
     * @param efficiencyKmPerGallon efficiency to add
     * @return the updated average
     */
    public VehicleFuelAverage register(BigDecimal efficiencyKmPerGallon) {
        return new VehicleFuelAverage(vehicleId, efficiencySum.add(efficiencyKmPerGallon), sampleCount + 1);
    }
}
