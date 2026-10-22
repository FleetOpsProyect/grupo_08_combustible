package co.edu.univalle.fleetops.fuel.adapter;

import static co.edu.univalle.fleetops.fuel.FuelTestData.BOGOTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.univalle.fleetops.fuel.adapter.in.scheduler.OutboxRelayScheduler;
import co.edu.univalle.fleetops.fuel.adapter.out.event.LoggingEventPublisherAdapter;
import co.edu.univalle.fleetops.fuel.application.port.in.RelayOutboxUseCase;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxMessage;
import co.edu.univalle.fleetops.fuel.utilities.AppConfig;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Scheduler, temporary publisher and wiring of the configuration classes. */
class SchedulerPublisherAndConfigTest {

    @Test
    void should_run_one_relay_cycle() {
        // Given
        RelayOutboxUseCase useCase = mock(RelayOutboxUseCase.class);
        when(useCase.relayPending()).thenReturn(2, 0);
        OutboxRelayScheduler scheduler = new OutboxRelayScheduler(useCase);

        // When / Then
        assertThat(scheduler.relay()).isEqualTo(2);
        assertThat(scheduler.relay()).isZero();
    }

    @Test
    void should_publish_to_log_without_failing() {
        LoggingEventPublisherAdapter publisher = new LoggingEventPublisherAdapter();
        OutboxMessage message = new OutboxMessage(UUID.randomUUID(), "fuel_anomaly_detected_event", "{}");

        assertThatCode(() -> publisher.publish(message)).doesNotThrowAnyException();
    }

    @Test
    void should_wire_core_and_outbound_rest_adapters_from_configuration() {
        new ApplicationContextRunner()
                .withUserConfiguration(PortsStub.class,
                        co.edu.univalle.fleetops.fuel.utilities.BeanConfiguration.class,
                        co.edu.univalle.fleetops.fuel.utilities.ClockProvider.class,
                        co.edu.univalle.fleetops.fuel.utilities.OpenApiDocs.class,
                        co.edu.univalle.fleetops.fuel.adapter.out.rest.Resilience4jConfig.class,
                        co.edu.univalle.fleetops.fuel.crosscutting.CorrelationIdPropagationInterceptor.class)
                .withPropertyValues(
                        "fuel.time-zone=America/Bogota",
                        "fuel.anomaly.threshold-percent=30", "fuel.anomaly.minimum-samples=2",
                        "fuel.outbox.relay-interval-ms=5000", "fuel.outbox.batch-size=50",
                        "fuel.assignments.base-url=http://assignments.test",
                        "fuel.assignments.in-progress-state=IN_PROGRESS",
                        "fuel.routes.base-url=http://routes.test", "fuel.routes.in-progress-status=IN_PROGRESS",
                        "fuel.http.timeout-ms=2000",
                        "fuel.circuit-breaker.failure-rate-threshold=50",
                        "fuel.circuit-breaker.sliding-window-size=10",
                        "fuel.circuit-breaker.minimum-calls=5",
                        "fuel.circuit-breaker.wait-open-ms=30000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AppConfig.class).anomaly().thresholdPercent())
                            .isEqualByComparingTo(BigDecimal.valueOf(30));
                    assertThat(context.getBean(java.time.Clock.class).getZone()).isEqualTo(BOGOTA);
                    assertThat(context).hasBean("registerFuelRecordService")
                            .hasBean("queryFuelRecordsService")
                            .hasBean("relayOutboxService")
                            .hasBean("assignmentRestClient")
                            .hasBean("routeRestClient")
                            .hasBean("fuelOpenApi");
                });
    }

    /** Persistence and publisher ports as mocks, plus the RestClient.Builder that Spring Boot provides. */
    @Configuration
    static class PortsStub {
        @Bean
        co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort fuelRecordRepositoryPort() {
            return mock(co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort.class);
        }

        @Bean
        co.edu.univalle.fleetops.fuel.application.port.out.VehicleAveragePort vehicleAveragePort() {
            return mock(co.edu.univalle.fleetops.fuel.application.port.out.VehicleAveragePort.class);
        }

        @Bean
        co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort outboxPort() {
            return mock(co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort.class);
        }

        @Bean
        co.edu.univalle.fleetops.fuel.application.port.out.EventPublisherPort eventPublisherPort() {
            return mock(co.edu.univalle.fleetops.fuel.application.port.out.EventPublisherPort.class);
        }

        @Bean
        org.springframework.web.client.RestClient.Builder restClientBuilder() {
            return org.springframework.web.client.RestClient.builder();
        }
    }
}
