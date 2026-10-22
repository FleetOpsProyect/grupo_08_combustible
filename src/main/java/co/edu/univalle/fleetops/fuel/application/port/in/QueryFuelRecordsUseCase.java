package co.edu.univalle.fleetops.fuel.application.port.in;

import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.PageQuery;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import java.util.UUID;

/** Inbound port: read fuel records (Fleet Administrator and Reports). */
public interface QueryFuelRecordsUseCase {

    /**
     * Finds one fuel record.
     *
     * @param fuelRecordId id of the fuel record
     * @return the fuel record
     */
    FuelRecord findById(UUID fuelRecordId);

    /**
     * Lists fuel records with filters, plus the summary block used by Reports.
     *
     * @param filter optional filters
     * @param page   page and size
     * @return the page and the summary of all the records that match the filters
     */
    FuelRecordPage search(FuelRecordFilter filter, PageQuery page);
}
