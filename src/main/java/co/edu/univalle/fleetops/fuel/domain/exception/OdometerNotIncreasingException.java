package co.edu.univalle.fleetops.fuel.domain.exception;

import java.math.BigDecimal;

/** Business rule violated: the odometer reading is not greater than the previous one of the vehicle (422). */
public class OdometerNotIncreasingException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param previousOdometerKm last odometer reading of the vehicle
     * @param currentOdometerKm  rejected odometer reading
     */
    public OdometerNotIncreasingException(BigDecimal previousOdometerKm, BigDecimal currentOdometerKm) {
        super("odometer_km " + currentOdometerKm.toPlainString()
                + " must be greater than the previous reading " + previousOdometerKm.toPlainString());
    }
}
