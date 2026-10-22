package co.edu.univalle.fleetops.fuel.application.query;

import java.math.BigDecimal;

/** Summary block that Reports (G10) reads, so it does not have to walk every page. */
public record FuelRecordSummary(
        BigDecimal totalCost,
        BigDecimal totalGallons,
        long anomalousRecords,
        long totalRecords) {
}
