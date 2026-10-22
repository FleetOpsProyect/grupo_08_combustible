package co.edu.univalle.fleetops.fuel.domain;

import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ThresholdAnomalyRuleTest {

    private final AnomalyRule rule = new ThresholdAnomalyRule(decimal("30"), 2);

    /** Average of 20.00 km/gal built from two previous efficiencies. */
    private final VehicleFuelAverage historyOfTwo = new VehicleFuelAverage(VEHICLE_ID, decimal("40.00"), 2);

    @Test
    void should_mark_anomalous_when_efficiency_drops_more_than_threshold() {
        // Given: limit = 20.00 * 0.70 = 14.00
        BigDecimal efficiency = decimal("13.99");

        // When
        boolean anomalous = rule.isAnomalous(efficiency, historyOfTwo);

        // Then
        assertThat(anomalous).isTrue();
    }

    @Test
    void should_not_mark_anomalous_when_efficiency_is_at_the_limit() {
        // Given / When
        boolean anomalous = rule.isAnomalous(decimal("14.00"), historyOfTwo);

        // Then
        assertThat(anomalous).isFalse();
    }

    @Test
    void should_not_evaluate_when_vehicle_has_fewer_samples_than_minimum() {
        // Given
        VehicleFuelAverage historyOfOne = new VehicleFuelAverage(VEHICLE_ID, decimal("20.00"), 1);

        // When
        boolean anomalous = rule.isAnomalous(decimal("1.00"), historyOfOne);

        // Then
        assertThat(anomalous).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "100", "150"})
    void should_reject_threshold_outside_zero_and_one_hundred(String threshold) {
        // Given
        BigDecimal invalid = decimal(threshold);

        // When / Then
        assertThatThrownBy(() -> new ThresholdAnomalyRule(invalid, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_missing_threshold() {
        // Given / When / Then
        assertThatThrownBy(() -> new ThresholdAnomalyRule(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_minimum_samples_below_one() {
        // Given / When / Then
        assertThatThrownBy(() -> new ThresholdAnomalyRule(decimal("30"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumSamples");
    }
}
