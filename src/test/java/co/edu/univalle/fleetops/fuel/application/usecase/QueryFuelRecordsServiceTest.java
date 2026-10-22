package co.edu.univalle.fleetops.fuel.application.usecase;

import static co.edu.univalle.fleetops.fuel.FuelTestData.FUEL_RECORD_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static co.edu.univalle.fleetops.fuel.FuelTestData.fuelRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import co.edu.univalle.fleetops.fuel.application.exception.FuelRecordNotFoundException;
import co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordFilter;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordSummary;
import co.edu.univalle.fleetops.fuel.application.query.PageQuery;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryFuelRecordsServiceTest {

    @Mock
    private FuelRecordRepositoryPort fuelRecordRepository;

    @Test
    void should_return_record_when_it_exists() {
        // Given
        FuelRecord stored = fuelRecord(decimal("20.00"), false);
        when(fuelRecordRepository.findById(FUEL_RECORD_ID)).thenReturn(Optional.of(stored));
        QueryFuelRecordsService service = new QueryFuelRecordsService(fuelRecordRepository);

        // When
        FuelRecord result = service.findById(FUEL_RECORD_ID);

        // Then
        assertThat(result).isEqualTo(stored);
    }

    @Test
    void should_fail_with_not_found_when_record_does_not_exist() {
        // Given
        when(fuelRecordRepository.findById(FUEL_RECORD_ID)).thenReturn(Optional.empty());
        QueryFuelRecordsService service = new QueryFuelRecordsService(fuelRecordRepository);

        // When / Then
        assertThatThrownBy(() -> service.findById(FUEL_RECORD_ID))
                .isInstanceOf(FuelRecordNotFoundException.class)
                .hasMessageContaining(FUEL_RECORD_ID.toString());
    }

    @Test
    void should_return_page_and_summary_of_repository() {
        // Given
        FuelRecordFilter filter = new FuelRecordFilter(VEHICLE_ID, null, null, Boolean.TRUE, null, null);
        PageQuery page = new PageQuery(0, 20);
        FuelRecordPage expected = new FuelRecordPage(List.of(fuelRecord(decimal("8.00"), true)), 0, 20, 1,
                new FuelRecordSummary(decimal("187500.00"), decimal("12.500"), 1, 1));
        when(fuelRecordRepository.search(filter, page)).thenReturn(expected);
        QueryFuelRecordsService service = new QueryFuelRecordsService(fuelRecordRepository);

        // When
        FuelRecordPage result = service.search(filter, page);

        // Then
        assertThat(result).isEqualTo(expected);
        assertThat(result.summary().anomalousRecords()).isEqualTo(1);
    }
}
