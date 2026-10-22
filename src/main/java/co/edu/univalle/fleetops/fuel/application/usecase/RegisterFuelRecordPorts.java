package co.edu.univalle.fleetops.fuel.application.usecase;

import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentPort;
import co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort;
import co.edu.univalle.fleetops.fuel.application.port.out.RoutePort;
import co.edu.univalle.fleetops.fuel.application.port.out.VehicleAveragePort;
import java.util.Objects;

/** Outbound ports used by the register use case, grouped so the constructor stays short. */
public record RegisterFuelRecordPorts(
        AssignmentPort assignmentPort,
        RoutePort routePort,
        FuelRecordRepositoryPort fuelRecordRepository,
        VehicleAveragePort vehicleAveragePort,
        OutboxPort outboxPort) {

    public RegisterFuelRecordPorts {
        Objects.requireNonNull(assignmentPort, "assignmentPort");
        Objects.requireNonNull(routePort, "routePort");
        Objects.requireNonNull(fuelRecordRepository, "fuelRecordRepository");
        Objects.requireNonNull(vehicleAveragePort, "vehicleAveragePort");
        Objects.requireNonNull(outboxPort, "outboxPort");
    }
}
