package co.edu.univalle.fleetops.fuel.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;

/** DTO of GET /fuel_records: page plus the summary block that Reports (G10) reads. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FuelRecordPageResponse(
        List<FuelRecordResponse> content,
        int page,
        int size,
        long total,
        Summary summary) {

    /** Summary of every record that matches the filters, not only the current page. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Summary(BigDecimal totalCost, BigDecimal totalGallons, long anomalousRecords, long totalRecords) {
    }
}
