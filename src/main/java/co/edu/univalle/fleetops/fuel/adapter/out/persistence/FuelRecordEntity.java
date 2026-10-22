package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** JPA entity of the fuel_records table. It never leaves the persistence adapter. */
@Entity
@Table(name = "fuel_records")
public class FuelRecordEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "gallons_purchased", nullable = false)
    private BigDecimal gallonsPurchased;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @Column(name = "odometer_km", nullable = false)
    private BigDecimal odometerKm;

    @Column(name = "full_tank", nullable = false)
    private boolean fullTank;

    @Column(name = "efficiency_km_per_gallon")
    private BigDecimal efficiencyKmPerGallon;

    @Column(name = "anomalous_consumption", nullable = false)
    private boolean anomalousConsumption;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    protected FuelRecordEntity() {
        // Required by JPA
    }

    FuelRecordEntity(UUID id, UUID assignmentId, UUID vehicleId, UUID driverId, UUID routeId) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.routeId = routeId;
    }

    void setLoad(BigDecimal gallons, BigDecimal cost, BigDecimal odometer, boolean full) {
        this.gallonsPurchased = gallons;
        this.totalCost = cost;
        this.odometerKm = odometer;
        this.fullTank = full;
    }

    void setResult(BigDecimal efficiency, boolean anomalous, OffsetDateTime recorded) {
        this.efficiencyKmPerGallon = efficiency;
        this.anomalousConsumption = anomalous;
        this.recordedAt = recorded;
    }

    UUID getId() {
        return id;
    }

    UUID getAssignmentId() {
        return assignmentId;
    }

    UUID getVehicleId() {
        return vehicleId;
    }

    UUID getDriverId() {
        return driverId;
    }

    UUID getRouteId() {
        return routeId;
    }

    BigDecimal getGallonsPurchased() {
        return gallonsPurchased;
    }

    BigDecimal getTotalCost() {
        return totalCost;
    }

    BigDecimal getOdometerKm() {
        return odometerKm;
    }

    boolean isFullTank() {
        return fullTank;
    }

    BigDecimal getEfficiencyKmPerGallon() {
        return efficiencyKmPerGallon;
    }

    boolean isAnomalousConsumption() {
        return anomalousConsumption;
    }

    OffsetDateTime getRecordedAt() {
        return recordedAt;
    }
}
