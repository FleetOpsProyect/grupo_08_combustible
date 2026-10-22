package co.edu.univalle.fleetops.fuel.domain;

import static co.edu.univalle.fleetops.fuel.FuelTestData.FUEL_RECORD_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.NOW;
import static co.edu.univalle.fleetops.fuel.FuelTestData.ROUTE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static co.edu.univalle.fleetops.fuel.FuelTestData.fuelRecord;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FuelAnomalyDetectedTest {

    @Test
    void should_copy_record_data_and_vehicle_average() {
        // Given
        FuelRecord anomalousRecord = fuelRecord(decimal("10.00"), true);
        VehicleFuelAverage average = new VehicleFuelAverage(VEHICLE_ID, decimal("40.00"), 2);

        // When
        FuelAnomalyDetected event = FuelAnomalyDetected.of(anomalousRecord, average);

        // Then
        assertThat(event.fuelRecordId()).isEqualTo(FUEL_RECORD_ID);
        assertThat(event.routeId()).isEqualTo(ROUTE_ID);
        assertThat(event.efficiencyKmPerGallon()).isEqualByComparingTo("10.00");
        assertThat(event.averageEfficiencyKmPerGallon()).isEqualByComparingTo("20.00");
        assertThat(event.recordedAt()).isEqualTo(NOW);
    }
}
