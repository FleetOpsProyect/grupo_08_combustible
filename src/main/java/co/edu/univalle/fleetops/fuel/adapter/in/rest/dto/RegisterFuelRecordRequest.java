package co.edu.univalle.fleetops.fuel.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO of POST /fuel_records (Integrity: the driver only sends the refueling data; vehicle, driver and
 * route are taken from the assignment). Validator pattern with Bean Validation.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterFuelRecordRequest(
        @NotNull UUID assignmentId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 3)
        BigDecimal gallonsPurchased,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
        BigDecimal totalCost,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 1)
        BigDecimal odometerKm,
        @NotNull Boolean fullTank) {
}
