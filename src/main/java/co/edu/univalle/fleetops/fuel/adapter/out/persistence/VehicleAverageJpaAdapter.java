package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import co.edu.univalle.fleetops.fuel.application.port.out.VehicleAveragePort;
import co.edu.univalle.fleetops.fuel.domain.VehicleFuelAverage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Outbound adapter: average efficiency per vehicle with Spring Data JPA. */
@Component
public class VehicleAverageJpaAdapter implements VehicleAveragePort {

    private final VehicleFuelAverageJpaRepository repository;

    /**
     * Creates the adapter.
     *
     * @param repository Spring Data repository
     */
    public VehicleAverageJpaAdapter(VehicleFuelAverageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<VehicleFuelAverage> findByVehicleId(UUID vehicleId) {
        return repository.findById(vehicleId).map(VehicleAverageJpaAdapter::toDomain);
    }

    @Override
    public VehicleFuelAverage save(VehicleFuelAverage average) {
        VehicleFuelAverageEntity entity = repository.findById(average.vehicleId())
                .orElseGet(() -> new VehicleFuelAverageEntity(average.vehicleId()));
        entity.update(average.efficiencySum(), average.sampleCount());
        return toDomain(repository.save(entity));
    }

    private static VehicleFuelAverage toDomain(VehicleFuelAverageEntity entity) {
        return new VehicleFuelAverage(entity.getVehicleId(), entity.getEfficiencySum(), entity.getSampleCount());
    }
}
