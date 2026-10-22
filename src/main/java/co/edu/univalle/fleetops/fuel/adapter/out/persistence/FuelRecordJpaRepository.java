package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository of fuel_records. Used only by {@link FuelRecordJpaAdapter}. */
interface FuelRecordJpaRepository extends JpaRepository<FuelRecordEntity, UUID>,
        JpaSpecificationExecutor<FuelRecordEntity> {

    @Query("select max(f.odometerKm) from FuelRecordEntity f where f.vehicleId = :vehicleId")
    BigDecimal findMaxOdometerKm(@Param("vehicleId") UUID vehicleId);

    Optional<FuelRecordEntity> findFirstByVehicleIdAndFullTankTrueOrderByOdometerKmDesc(UUID vehicleId);

    @Query("select coalesce(sum(f.gallonsPurchased), 0) from FuelRecordEntity f "
            + "where f.vehicleId = :vehicleId and f.odometerKm > :odometerKm")
    BigDecimal sumGallonsAfter(@Param("vehicleId") UUID vehicleId, @Param("odometerKm") BigDecimal odometerKm);
}
