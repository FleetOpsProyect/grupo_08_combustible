package co.edu.univalle.fleetops.fuel.application.exception;

import java.util.UUID;

/** The fuel record does not exist (404). */
public class FuelRecordNotFoundException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param fuelRecordId id that was looked up
     */
    public FuelRecordNotFoundException(UUID fuelRecordId) {
        super("Fuel record not found: " + fuelRecordId);
    }
}
