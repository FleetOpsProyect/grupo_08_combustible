package co.edu.univalle.fleetops.fuel.application.query;

import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import java.util.List;

/** One page of fuel records and the summary of every record that matches the filters. */
public record FuelRecordPage(
        List<FuelRecord> content,
        int page,
        int size,
        long total,
        FuelRecordSummary summary) {
}
