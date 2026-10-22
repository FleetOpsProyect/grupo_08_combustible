package co.edu.univalle.fleetops.fuel.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Domain service: efficiency in km per gallon between two full tanks.
 * Kilometers since the last full tank divided by all the gallons loaded in that stretch.
 */
public class EfficiencyCalculator {

    private static final int SCALE = 2;

    /**
     * Calculates the efficiency of a full-tank refueling.
     *
     * @param currentOdometerKm odometer reading of the current refueling
     * @param currentGallons    gallons loaded in the current refueling
     * @param segment           stretch since the last full tank; empty if the vehicle has none
     * @return km per gallon, or empty when there is no previous full tank to compare with
     */
    public Optional<BigDecimal> calculate(BigDecimal currentOdometerKm,
                                          BigDecimal currentGallons,
                                          Optional<TankSegment> segment) {
        return segment.map(stretch -> {
            BigDecimal kilometers = currentOdometerKm.subtract(stretch.startOdometerKm());
            BigDecimal gallons = stretch.gallonsSinceStart().add(currentGallons);
            return kilometers.divide(gallons, SCALE, RoundingMode.HALF_UP);
        });
    }
}
