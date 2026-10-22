package co.edu.univalle.fleetops.fuel.domain;

import java.math.BigDecimal;

/**
 * Stretch driven since the last full tank of a vehicle.
 *
 * @param startOdometerKm      odometer reading of the last full tank
 * @param gallonsSinceStart    gallons loaded after that full tank (partial loads), without the current one
 */
public record TankSegment(BigDecimal startOdometerKm, BigDecimal gallonsSinceStart) {
}
