package co.edu.univalle.fleetops.fuel.domain;

import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VehicleFuelAverageTest {

    @Test
    void should_start_without_samples_and_zero_average() {
        // Given / When
        VehicleFuelAverage average = VehicleFuelAverage.empty(VEHICLE_ID);

        // Then
        assertThat(average.sampleCount()).isZero();
        assertThat(average.averageKmPerGallon()).isEqualByComparingTo("0");
    }

    @Test
    void should_add_efficiency_and_recalculate_average() {
        // Given
        VehicleFuelAverage average = new VehicleFuelAverage(VEHICLE_ID, decimal("40.00"), 2);

        // When
        VehicleFuelAverage updated = average.register(decimal("26.00"));

        // Then: (40 + 26) / 3 = 22.00
        assertThat(updated.sampleCount()).isEqualTo(3);
        assertThat(updated.efficiencySum()).isEqualByComparingTo("66.00");
        assertThat(updated.averageKmPerGallon()).isEqualByComparingTo("22.00");
    }

    @Test
    void should_reject_negative_sample_count() {
        // Given / When / Then
        assertThatThrownBy(() -> new VehicleFuelAverage(VEHICLE_ID, decimal("1"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
