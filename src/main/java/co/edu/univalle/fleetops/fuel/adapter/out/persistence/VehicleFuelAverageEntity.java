package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA entity of the vehicle_fuel_averages table.
 * [Archetype Convention Addition] @Version (optimistic lock, Fowler) so two refuelings of the same vehicle
 * at the same time cannot overwrite the average.
 */
@Entity
@Table(name = "vehicle_fuel_averages")
public class VehicleFuelAverageEntity {

    @Id
    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "efficiency_sum", nullable = false)
    private BigDecimal efficiencySum;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Version
    @Column(name = "version")
    private Long version;

    protected VehicleFuelAverageEntity() {
        // Required by JPA
    }

    VehicleFuelAverageEntity(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    void update(BigDecimal sum, int count) {
        this.efficiencySum = sum;
        this.sampleCount = count;
    }

    UUID getVehicleId() {
        return vehicleId;
    }

    BigDecimal getEfficiencySum() {
        return efficiencySum;
    }

    int getSampleCount() {
        return sampleCount;
    }
}
