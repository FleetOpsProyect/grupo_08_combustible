package co.edu.univalle.fleetops.fuel.utilities;

import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentPort;
import co.edu.univalle.fleetops.fuel.application.port.out.EventPublisherPort;
import co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort;
import co.edu.univalle.fleetops.fuel.application.port.out.RoutePort;
import co.edu.univalle.fleetops.fuel.application.port.out.VehicleAveragePort;
import co.edu.univalle.fleetops.fuel.application.usecase.QueryFuelRecordsService;
import co.edu.univalle.fleetops.fuel.application.usecase.RegisterFuelRecordPorts;
import co.edu.univalle.fleetops.fuel.application.usecase.RegisterFuelRecordService;
import co.edu.univalle.fleetops.fuel.application.usecase.RelayOutboxService;
import co.edu.univalle.fleetops.fuel.domain.AnomalyRule;
import co.edu.univalle.fleetops.fuel.domain.EfficiencyCalculator;
import co.edu.univalle.fleetops.fuel.domain.ThresholdAnomalyRule;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Dependency injection made explicit: here the core (domain and use cases, which have no Spring stereotypes)
 * is connected to the adapters through the ports.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AppConfig.class)
public class BeanConfiguration {

    @Bean
    EfficiencyCalculator efficiencyCalculator() {
        return new EfficiencyCalculator();
    }

    @Bean
    AnomalyRule anomalyRule(AppConfig config) {
        return new ThresholdAnomalyRule(config.anomaly().thresholdPercent(), config.anomaly().minimumSamples());
    }

    @Bean
    @SuppressWarnings("java:S107")
    RegisterFuelRecordService registerFuelRecordService(AssignmentPort assignmentPort,
                                                        RoutePort routePort,
                                                        FuelRecordRepositoryPort fuelRecordRepository,
                                                        VehicleAveragePort vehicleAveragePort,
                                                        OutboxPort outboxPort,
                                                        EfficiencyCalculator efficiencyCalculator,
                                                        AnomalyRule anomalyRule,
                                                        Clock clock,
                                                        Supplier<UUID> idGenerator) {
        RegisterFuelRecordPorts ports = new RegisterFuelRecordPorts(assignmentPort, routePort,
                fuelRecordRepository, vehicleAveragePort, outboxPort);
        return new RegisterFuelRecordService(ports, efficiencyCalculator, anomalyRule, clock, idGenerator);
    }

    @Bean
    QueryFuelRecordsService queryFuelRecordsService(FuelRecordRepositoryPort fuelRecordRepository) {
        return new QueryFuelRecordsService(fuelRecordRepository);
    }

    @Bean
    RelayOutboxService relayOutboxService(OutboxPort outboxPort, EventPublisherPort eventPublisher, Clock clock,
                                          AppConfig config) {
        return new RelayOutboxService(outboxPort, eventPublisher, clock, config.outbox().batchSize());
    }
}
