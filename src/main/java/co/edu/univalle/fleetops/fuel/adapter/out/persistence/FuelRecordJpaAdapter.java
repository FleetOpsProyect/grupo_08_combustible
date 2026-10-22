package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordSummary;
import co.edu.univalle.fleetops.fuel.application.query.PageQuery;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import co.edu.univalle.fleetops.fuel.domain.TankSegment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Outbound adapter (Repository): implements the fuel record port with Spring Data JPA on PostgreSQL. */
@Component
public class FuelRecordJpaAdapter implements FuelRecordRepositoryPort {

    private final FuelRecordJpaRepository repository;
    private final EntityManager entityManager;
    private final Clock clock;

    /**
     * Creates the adapter.
     *
     * @param repository    Spring Data repository
     * @param entityManager used for the summary aggregate query
     * @param clock         clock whose zone (Colombia) is applied to the dates read from the database
     */
    public FuelRecordJpaAdapter(FuelRecordJpaRepository repository, EntityManager entityManager, Clock clock) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public FuelRecord save(FuelRecord fuelRecord) {
        return toDomain(repository.save(toEntity(fuelRecord)));
    }

    @Override
    public Optional<FuelRecord> findById(UUID fuelRecordId) {
        return repository.findById(fuelRecordId).map(this::toDomain);
    }

    @Override
    public Optional<BigDecimal> findLastOdometerKm(UUID vehicleId) {
        return Optional.ofNullable(repository.findMaxOdometerKm(vehicleId));
    }

    @Override
    public Optional<TankSegment> findOpenTankSegment(UUID vehicleId) {
        return repository.findFirstByVehicleIdAndFullTankTrueOrderByOdometerKmDesc(vehicleId)
                .map(lastFull -> new TankSegment(
                        lastFull.getOdometerKm(),
                        repository.sumGallonsAfter(vehicleId, lastFull.getOdometerKm())));
    }

    @Override
    public FuelRecordPage search(FuelRecordFilter filter, PageQuery page) {
        Specification<FuelRecordEntity> specification = FuelRecordSpecifications.matching(filter);
        Page<FuelRecordEntity> result = repository.findAll(specification,
                PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "recordedAt")));
        return new FuelRecordPage(
                result.getContent().stream().map(this::toDomain).toList(),
                page.page(),
                page.size(),
                result.getTotalElements(),
                summarize(specification));
    }

    private FuelRecordSummary summarize(Specification<FuelRecordEntity> specification) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<FuelRecordEntity> root = query.from(FuelRecordEntity.class);
        Expression<Long> anomalousAsNumber = builder.<Long>selectCase()
                .when(builder.isTrue(root.get("anomalousConsumption")), 1L)
                .otherwise(0L);
        query.multiselect(
                builder.coalesce(builder.sum(root.<BigDecimal>get("totalCost")), BigDecimal.ZERO),
                builder.coalesce(builder.sum(root.<BigDecimal>get("gallonsPurchased")), BigDecimal.ZERO),
                builder.coalesce(builder.sum(anomalousAsNumber), 0L),
                builder.count(root));
        query.where(specification.toPredicate(root, query, builder));
        Tuple totals = entityManager.createQuery(query).getSingleResult();
        return new FuelRecordSummary(
                totals.get(0, BigDecimal.class),
                totals.get(1, BigDecimal.class),
                totals.get(2, Long.class),
                totals.get(3, Long.class));
    }

    private FuelRecordEntity toEntity(FuelRecord fuelRecord) {
        FuelRecordEntity entity = new FuelRecordEntity(fuelRecord.id(), fuelRecord.assignmentId(),
                fuelRecord.vehicleId(), fuelRecord.driverId(), fuelRecord.routeId());
        entity.setLoad(fuelRecord.gallonsPurchased(), fuelRecord.totalCost(), fuelRecord.odometerKm(),
                fuelRecord.fullTank());
        entity.setResult(fuelRecord.efficiencyKmPerGallon(), fuelRecord.anomalousConsumption(),
                fuelRecord.recordedAt());
        return entity;
    }

    private FuelRecord toDomain(FuelRecordEntity entity) {
        return new FuelRecord(entity.getId(), entity.getAssignmentId(), entity.getVehicleId(),
                entity.getDriverId(), entity.getRouteId(), entity.getGallonsPurchased(), entity.getTotalCost(),
                entity.getOdometerKm(), entity.isFullTank(), entity.getEfficiencyKmPerGallon(),
                entity.isAnomalousConsumption(), inLocalZone(entity.getRecordedAt()));
    }

    private OffsetDateTime inLocalZone(OffsetDateTime value) {
        return value.atZoneSameInstant(clock.getZone()).toOffsetDateTime();
    }
}
