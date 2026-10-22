package co.edu.univalle.fleetops.fuel.application.port.in;

import co.edu.univalle.fleetops.fuel.domain.FuelRecord;

/** Inbound port: register a refueling made during a route in progress. */
public interface RegisterFuelRecordUseCase {

    /**
     * Registers a fuel record.
     *
     * @param command data sent by the driver
     * @return the stored fuel record, with its efficiency and anomaly flag
     */
    FuelRecord register(RegisterFuelRecordCommand command);
}
