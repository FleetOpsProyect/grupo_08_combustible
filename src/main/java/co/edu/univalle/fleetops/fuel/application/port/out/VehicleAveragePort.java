package co.edu.univalle.fleetops.fuel.application.port.out;

import co.edu.univalle.fleetops.fuel.domain.VehicleFuelAverage;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: average efficiency per vehicle, stored in its own table. */
public interface VehicleAveragePort {

    Optional<VehicleFuelAverage> findByVehicleId(UUID vehicleId);

    VehicleFuelAverage save(VehicleFuelAverage average);
}
