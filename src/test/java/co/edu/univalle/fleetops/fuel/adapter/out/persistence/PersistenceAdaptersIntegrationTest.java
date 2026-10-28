package co.edu.univalle.fleetops.fuel.adapter.out.persistence;

import static co.edu.univalle.fleetops.fuel.FuelTestData.ASSIGNMENT_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.DRIVER_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.FIXED_CLOCK;
import static co.edu.univalle.fleetops.fuel.FuelTestData.NOW;
import static co.edu.univalle.fleetops.fuel.FuelTestData.ROUTE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.univalle.fleetops.fuel.application.port.out.OutboxMessage;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.PageQuery;
import co.edu.univalle.fleetops.fuel.crosscutting.CorrelationIdFilter;
import co.edu.univalle.fleetops.fuel.domain.FuelAnomalyDetected;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import co.edu.univalle.fleetops.fuel.domain.VehicleFuelAverage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * [Archetype Convention Addition] Integration test of the persistence adapters against a real PostgreSQL 16
 * (Testcontainers, as in the course guide). Flyway creates the schema, so the migrations are tested too.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import({FuelRecordJpaAdapter.class, VehicleAverageJpaAdapter.class, OutboxJpaAdapter.class,
    PersistenceAdaptersIntegrationTest.Support.class})
class PersistenceAdaptersIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.15-alpine3.24");

    @Autowired
    private FuelRecordJpaAdapter fuelRecords;
    @Autowired
    private VehicleAverageJpaAdapter averages;
    @Autowired
    private OutboxJpaAdapter outbox;

    @TestConfiguration
    static class Support {
        @Bean
        Clock clock() {
            return FIXED_CLOCK;
        }

        @Bean
        Supplier<UUID> idGenerator() {
            return UUID::randomUUID;
        }

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().findAndAddModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        }
    }

    private static FuelRecord record(String odometer, String gallons, boolean full, BigDecimal efficiency,
                                     boolean anomalous, OffsetDateTime when) {
        return new FuelRecord(UUID.randomUUID(), ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, ROUTE_ID, decimal(gallons),
                decimal("100000.00"), decimal(odometer), full, efficiency, anomalous, when);
    }

    @Test
    void should_store_and_read_record_in_colombia_time_zone() {
        // Given
        FuelRecord saved = fuelRecords.save(record("1000.0", "10.000", true, null, false, NOW));

        // When / Then
        assertThat(fuelRecords.findById(saved.id())).hasValueSatisfying(found -> {
            assertThat(found.gallonsPurchased()).isEqualByComparingTo("10.000");
            assertThat(found.recordedAt()).isEqualTo(NOW);
            assertThat(found.recordedAt().getOffset().getTotalSeconds()).isEqualTo(-5 * 3600);
        });
        assertThat(fuelRecords.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_find_last_odometer_and_stretch_since_last_full_tank() {
        // Given: no records yet
        assertThat(fuelRecords.findLastOdometerKm(VEHICLE_ID)).isEmpty();
        assertThat(fuelRecords.findOpenTankSegment(VEHICLE_ID)).isEmpty();

        // When: full tank at 1000, then two partial loads of 3 and 2 gallons
        fuelRecords.save(record("1000.0", "10.000", true, null, false, NOW.minusHours(3)));
        fuelRecords.save(record("1100.0", "3.000", false, null, false, NOW.minusHours(2)));
        fuelRecords.save(record("1200.0", "2.000", false, null, false, NOW.minusHours(1)));

        // Then
        assertThat(fuelRecords.findLastOdometerKm(VEHICLE_ID)).hasValueSatisfying(
                value -> assertThat(value).isEqualByComparingTo("1200.0"));
        assertThat(fuelRecords.findOpenTankSegment(VEHICLE_ID)).hasValueSatisfying(segment -> {
            assertThat(segment.startOdometerKm()).isEqualByComparingTo("1000.0");
            assertThat(segment.gallonsSinceStart()).isEqualByComparingTo("5.000");
        });
    }

    @Test
    void should_filter_page_and_summarize_all_matching_records() {
        // Given
        fuelRecords.save(record("2000.0", "10.000", true, decimal("20.00"), false, NOW.minusDays(2)));
        fuelRecords.save(record("2200.0", "12.000", true, decimal("8.00"), true, NOW.minusDays(1)));
        fuelRecords.save(record("2300.0", "5.000", false, null, false, NOW));

        // When: page of one element, filtered by vehicle and date
        FuelRecordPage page = fuelRecords.search(new FuelRecordFilter(VEHICLE_ID, ASSIGNMENT_ID, ROUTE_ID, null,
                NOW.minusDays(3), NOW), new PageQuery(0, 1));
        FuelRecordPage anomalousOnly = fuelRecords.search(new FuelRecordFilter(null, null, null, true, null, null),
                new PageQuery(0, 20));

        // Then: the summary covers the three records, not only the page
        assertThat(page.content()).hasSize(1);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.summary().totalRecords()).isEqualTo(3);
        assertThat(page.summary().anomalousRecords()).isEqualTo(1);
        assertThat(page.summary().totalGallons()).isEqualByComparingTo("27.000");
        assertThat(page.summary().totalCost()).isEqualByComparingTo("300000.00");
        assertThat(anomalousOnly.total()).isEqualTo(1);
    }

    @Test
    void should_return_zero_summary_when_nothing_matches() {
        FuelRecordPage empty = fuelRecords.search(new FuelRecordFilter(UUID.randomUUID(), null, null, null, null,
                null), new PageQuery(0, 20));

        assertThat(empty.content()).isEmpty();
        assertThat(empty.summary().totalCost()).isEqualByComparingTo("0");
        assertThat(empty.summary().anomalousRecords()).isZero();
    }

    @Test
    void should_create_and_update_vehicle_average() {
        // Given
        assertThat(averages.findByVehicleId(VEHICLE_ID)).isEmpty();

        // When
        averages.save(VehicleFuelAverage.empty(VEHICLE_ID).register(decimal("20.00")));
        VehicleFuelAverage updated = averages.save(
                averages.findByVehicleId(VEHICLE_ID).orElseThrow().register(decimal("24.00")));

        // Then
        assertThat(updated.sampleCount()).isEqualTo(2);
        assertThat(updated.averageKmPerGallon()).isEqualByComparingTo("22.00");
    }

    @Test
    void should_store_contract_message_and_mark_it_published() {
        // Given
        FuelRecord anomalous = fuelRecords.save(record("3000.0", "12.000", true, decimal("8.00"), true, NOW));
        VehicleFuelAverage average = new VehicleFuelAverage(VEHICLE_ID, decimal("40.00"), 2);
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-fuel-1");

        // When
        try {
            outbox.save(FuelAnomalyDetected.of(anomalous, average), NOW);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
        List<OutboxMessage> pending = outbox.findPending(10);

        // Then
        assertThat(pending).hasSize(1);
        OutboxMessage message = pending.get(0);
        assertThat(message.eventType()).isEqualTo("fuel_anomaly_detected_event");
        assertThat(message.payload())
                .contains("\"event_type\"", "\"occurred_at\"", "\"source\"", "\"fuel\"")
                .contains("\"correlation_id\":\"corr-fuel-1\"")
                .contains("\"fuel_record_id\"", "\"average_efficiency_km_per_gallon\"")
                .contains("-05:00");

        outbox.markPublished(message.eventId(), NOW);
        outbox.markPublished(UUID.randomUUID(), NOW);
        assertThat(outbox.findPending(10)).isEmpty();
    }

    @Test
    void should_generate_correlation_id_when_there_is_no_request() {
        // Given
        FuelRecord anomalous = fuelRecords.save(record("3100.0", "12.000", true, decimal("8.00"), true, NOW));
        VehicleFuelAverage average = new VehicleFuelAverage(VEHICLE_ID, decimal("40.00"), 2);

        // When
        outbox.save(FuelAnomalyDetected.of(anomalous, average), NOW);

        // Then
        assertThat(outbox.findPending(10).get(0).payload()).containsPattern("\"correlation_id\":\"[0-9a-f-]{36}\"");
    }
}
