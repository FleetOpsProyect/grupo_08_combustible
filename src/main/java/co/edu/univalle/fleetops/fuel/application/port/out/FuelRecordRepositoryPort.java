package co.edu.univalle.fleetops.fuel.application.port.out;

import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.PageQuery;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import co.edu.univalle.fleetops.fuel.domain.TankSegment;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Outbound port (Repository): fuel records stored in the service database. */
public interface FuelRecordRepositoryPort {

    FuelRecord save(FuelRecord fuelRecord);

    Optional<FuelRecord> findById(UUID fuelRecordId);

    /**
     * Highest odometer reading registered for a vehicle.
     *
     * @param vehicleId vehicle
     * @return the reading, or empty when the vehicle has no records
     */
    Optional<BigDecimal> findLastOdometerKm(UUID vehicleId);

    /**
     * Stretch since the last full tank of a vehicle.
     *
     * @param vehicleId vehicle
     * @return the stretch, or empty when the vehicle never filled the tank
     */
    Optional<TankSegment> findOpenTankSegment(UUID vehicleId);

    /**
     * Page of records plus the summary of every record that matches the filters (one aggregate query).
     *
     * @param filter optional filters
     * @param page   page and size
     * @return page and summary
     */
    FuelRecordPage search(FuelRecordFilter filter, PageQuery page);
}
