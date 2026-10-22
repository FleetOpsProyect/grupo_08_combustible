package co.edu.univalle.fleetops.fuel.domain;

import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EfficiencyCalculatorTest {

    private final EfficiencyCalculator calculator = new EfficiencyCalculator();

    @Test
    void should_divide_kilometers_by_all_gallons_since_last_full_tank() {
        // Given: last full tank at 45000 km, 4 gallons of partial loads after it, 12 gallons now at 45400 km
        Optional<TankSegment> segment = Optional.of(new TankSegment(decimal("45000.0"), decimal("4.000")));

        // When
        Optional<BigDecimal> efficiency = calculator.calculate(decimal("45400.0"), decimal("12.000"), segment);

        // Then: 400 km / 16 gal = 25.00 km/gal
        assertThat(efficiency).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("25.00"));
    }

    @Test
    void should_round_efficiency_to_two_decimals() {
        // Given
        Optional<TankSegment> segment = Optional.of(new TankSegment(decimal("1000.0"), BigDecimal.ZERO));

        // When
        Optional<BigDecimal> efficiency = calculator.calculate(decimal("1100.0"), decimal("3.000"), segment);

        // Then: 100 / 3 = 33.333... -> 33.33
        assertThat(efficiency).contains(decimal("33.33"));
    }

    @Test
    void should_return_empty_when_vehicle_has_no_previous_full_tank() {
        // Given / When
        Optional<BigDecimal> efficiency = calculator.calculate(decimal("100.0"), decimal("10.0"), Optional.empty());

        // Then
        assertThat(efficiency).isEmpty();
    }
}
