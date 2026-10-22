package co.edu.univalle.fleetops.fuel.application.usecase;

import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotFoundException;
import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotInProgressException;
import co.edu.univalle.fleetops.fuel.application.exception.RouteNotInProgressException;
import co.edu.univalle.fleetops.fuel.application.port.in.RegisterFuelRecordCommand;
import co.edu.univalle.fleetops.fuel.application.port.in.RegisterFuelRecordUseCase;
import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentInfo;
import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentPort;
import co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort;
import co.edu.univalle.fleetops.fuel.application.port.out.RoutePort;
import co.edu.univalle.fleetops.fuel.application.port.out.VehicleAveragePort;
import co.edu.univalle.fleetops.fuel.domain.AnomalyRule;
import co.edu.univalle.fleetops.fuel.domain.EfficiencyCalculator;
import co.edu.univalle.fleetops.fuel.domain.FuelAnomalyDetected;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import co.edu.univalle.fleetops.fuel.domain.VehicleFuelAverage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: register a refueling.
 * Validates the assignment (G2) and the route (G6), calculates the efficiency, decides the anomaly and,
 * in one transaction, stores the record, updates the vehicle average or leaves the event in the outbox.
 */
public class RegisterFuelRecordService implements RegisterFuelRecordUseCase {

    private final AssignmentPort assignmentPort;
    private final RoutePort routePort;
    private final FuelRecordRepositoryPort fuelRecordRepository;
    private final VehicleAveragePort vehicleAveragePort;
    private final OutboxPort outboxPort;
    private final EfficiencyCalculator efficiencyCalculator;
    private final AnomalyRule anomalyRule;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    /**
     * Creates the use case. Every dependency is injected through the constructor so tests replace them.
     *
     * @param ports       outbound ports used by the use case
     * @param calculator  efficiency domain service
     * @param anomalyRule anomaly strategy
     * @param clock       clock in the Colombia time zone
     * @param idGenerator generator of fuel record ids
     */
    public RegisterFuelRecordService(RegisterFuelRecordPorts ports,
                                     EfficiencyCalculator calculator,
                                     AnomalyRule anomalyRule,
                                     Clock clock,
                                     Supplier<UUID> idGenerator) {
        this.assignmentPort = ports.assignmentPort();
        this.routePort = ports.routePort();
        this.fuelRecordRepository = ports.fuelRecordRepository();
        this.vehicleAveragePort = ports.vehicleAveragePort();
        this.outboxPort = ports.outboxPort();
        this.efficiencyCalculator = calculator;
        this.anomalyRule = anomalyRule;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public FuelRecord register(RegisterFuelRecordCommand command) {
        UUID assignmentId = command.assignmentId();
        AssignmentInfo assignment = assignmentPort.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
        if (!assignment.inProgress()) {
            throw new AssignmentNotInProgressException(assignmentId);
        }
        UUID routeId = routePort.findRouteInProgress(assignmentId)
                .orElseThrow(() -> new RouteNotInProgressException(assignmentId));

        UUID vehicleId = assignment.vehicleId();
        FuelRecord.ensureOdometerIncreases(fuelRecordRepository.findLastOdometerKm(vehicleId), command.odometerKm());

        Optional<BigDecimal> efficiency = command.fullTank()
                ? efficiencyCalculator.calculate(command.odometerKm(), command.gallonsPurchased(),
                        fuelRecordRepository.findOpenTankSegment(vehicleId))
                : Optional.empty();
        VehicleFuelAverage average = vehicleAveragePort.findByVehicleId(vehicleId)
                .orElseGet(() -> VehicleFuelAverage.empty(vehicleId));
        boolean anomalous = efficiency.map(value -> anomalyRule.isAnomalous(value, average)).orElse(false);
        OffsetDateTime now = OffsetDateTime.now(clock);

        FuelRecord saved = fuelRecordRepository.save(new FuelRecord(
                idGenerator.get(), assignmentId, vehicleId, assignment.driverId(), routeId,
                command.gallonsPurchased(), command.totalCost(), command.odometerKm(), command.fullTank(),
                efficiency.orElse(null), anomalous, now));

        if (anomalous) {
            outboxPort.save(FuelAnomalyDetected.of(saved, average), now);
        } else {
            efficiency.ifPresent(value -> vehicleAveragePort.save(average.register(value)));
        }
        return saved;
    }
}
