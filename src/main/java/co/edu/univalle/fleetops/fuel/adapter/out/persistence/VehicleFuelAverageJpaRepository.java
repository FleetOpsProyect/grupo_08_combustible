package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of vehicle_fuel_averages. Used only by {@link VehicleAverageJpaAdapter}. */
interface VehicleFuelAverageJpaRepository extends JpaRepository<VehicleFuelAverageEntity, UUID> {
}
