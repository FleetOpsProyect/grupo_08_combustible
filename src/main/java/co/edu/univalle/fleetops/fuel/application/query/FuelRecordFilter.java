package co.edu.univalle.fleetops.fuel.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Optional filters of the fuel record list. A null field means "no filter". */
public record FuelRecordFilter(
        UUID vehicleId,
        UUID assignmentId,
        UUID routeId,
        Boolean anomalousConsumption,
        OffsetDateTime dateFrom,
        OffsetDateTime dateTo) {
}
