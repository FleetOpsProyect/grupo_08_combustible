package co.edu.univalle.fleetops.fuel.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/** Input of the register use case. The driver only sends the assignment; vehicle, driver and route are derived. */
public record RegisterFuelRecordCommand(
        UUID assignmentId,
        BigDecimal gallonsPurchased,
        BigDecimal totalCost,
        BigDecimal odometerKm,
        boolean fullTank) {
}
