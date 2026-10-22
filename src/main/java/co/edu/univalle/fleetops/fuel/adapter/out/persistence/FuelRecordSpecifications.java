package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Translates the optional filters of the list into JPA predicates. */
final class FuelRecordSpecifications {

    private FuelRecordSpecifications() {
    }

    static Specification<FuelRecordEntity> matching(FuelRecordFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.vehicleId() != null) {
                predicates.add(builder.equal(root.get("vehicleId"), filter.vehicleId()));
            }
            if (filter.assignmentId() != null) {
                predicates.add(builder.equal(root.get("assignmentId"), filter.assignmentId()));
            }
            if (filter.routeId() != null) {
                predicates.add(builder.equal(root.get("routeId"), filter.routeId()));
            }
            if (filter.anomalousConsumption() != null) {
                predicates.add(builder.equal(root.get("anomalousConsumption"), filter.anomalousConsumption()));
            }
            if (filter.dateFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("recordedAt"), filter.dateFrom()));
            }
            if (filter.dateTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("recordedAt"), filter.dateTo()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
