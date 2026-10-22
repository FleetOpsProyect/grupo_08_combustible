package co.edu.univalle.fleetops.fuel.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DTO of one fuel record in the responses (English, snake_case, dates in GMT-5). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FuelRecordResponse(
        UUID id,
        UUID assignmentId,
        UUID vehicleId,
        UUID driverId,
        UUID routeId,
        BigDecimal gallonsPurchased,
        BigDecimal totalCost,
        BigDecimal odometerKm,
        boolean fullTank,
        BigDecimal efficiencyKmPerGallon,
        boolean anomalousConsumption,
        OffsetDateTime recordedAt) {
}
