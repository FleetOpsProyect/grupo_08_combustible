package co.edu.univalle.fleetops.fuel.domain;

import static co.edu.univalle.fleetops.fuel.FuelTestData.ASSIGNMENT_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.DRIVER_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.FUEL_RECORD_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.NOW;
import static co.edu.univalle.fleetops.fuel.FuelTestData.ROUTE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static co.edu.univalle.fleetops.fuel.FuelTestData.fuelRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.univalle.fleetops.fuel.domain.exception.InvalidFuelRecordException;
import co.edu.univalle.fleetops.fuel.domain.exception.OdometerNotIncreasingException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FuelRecordTest {

    @Test
    void should_create_record_when_all_values_are_valid() {
        // Given / When
        FuelRecord fuelRecord = fuelRecord(decimal("12.50"), false);

        // Then
        assertThat(fuelRecord.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(fuelRecord.efficiencyKmPerGallon()).isEqualByComparingTo("12.50");
        assertThat(fuelRecord.anomalousConsumption()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.5"})
    void should_reject_gallons_when_not_positive(String gallons) {
        // Given
        BigDecimal invalidGallons = decimal(gallons);

        // When / Then
        assertThatThrownBy(() -> new FuelRecord(FUEL_RECORD_ID, ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, ROUTE_ID,
                invalidGallons, decimal("1000"), decimal("100"), false, null, false, NOW))
                .isInstanceOf(InvalidFuelRecordException.class)
                .extracting(error -> ((InvalidFuelRecordException) error).field())
                .isEqualTo("gallons_purchased");
    }

    @Test
    void should_reject_total_cost_when_missing() {
        // Given / When / Then
        assertThatThrownBy(() -> new FuelRecord(FUEL_RECORD_ID, ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, ROUTE_ID,
                decimal("10"), null, decimal("100"), false, null, false, NOW))
                .isInstanceOf(InvalidFuelRecordException.class)
                .hasMessageContaining("total_cost");
    }

    @Test
    void should_reject_anomalous_record_without_efficiency() {
        // Given / When / Then
        assertThatThrownBy(() -> fuelRecord(null, true))
                .isInstanceOf(InvalidFuelRecordException.class)
                .hasMessageContaining("efficiency_km_per_gallon");
    }

    @Test
    void should_accept_odometer_when_vehicle_has_no_previous_reading() {
        // Given / When / Then
        assertThatCode(() -> FuelRecord.ensureOdometerIncreases(Optional.empty(), decimal("100.0")))
                .doesNotThrowAnyException();
    }

    @Test
    void should_accept_odometer_when_greater_than_previous_reading() {
        // Given / When / Then
        assertThatCode(() -> FuelRecord.ensureOdometerIncreases(Optional.of(decimal("100.0")), decimal("100.1")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"100.0", "99.9"})
    void should_reject_odometer_when_not_greater_than_previous_reading(String current) {
        // Given
        Optional<BigDecimal> previous = Optional.of(decimal("100.0"));

        // When / Then
        assertThatThrownBy(() -> FuelRecord.ensureOdometerIncreases(previous, decimal(current)))
                .isInstanceOf(OdometerNotIncreasingException.class)
                .hasMessageContaining("previous reading 100.0");
    }
}
