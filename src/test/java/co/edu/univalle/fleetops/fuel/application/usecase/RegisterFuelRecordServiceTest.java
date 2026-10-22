package co.edu.univalle.fleetops.fuel.application.usecase;

import static co.edu.univalle.fleetops.fuel.FuelTestData.ASSIGNMENT_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.DRIVER_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.FIXED_CLOCK;
import static co.edu.univalle.fleetops.fuel.FuelTestData.FUEL_RECORD_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.NOW;
import static co.edu.univalle.fleetops.fuel.FuelTestData.ROUTE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotFoundException;
import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotInProgressException;
import co.edu.univalle.fleetops.fuel.application.exception.RouteNotInProgressException;
import co.edu.univalle.fleetops.fuel.application.port.in.RegisterFuelRecordCommand;
import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentInfo;
import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentPort;
import co.edu.univalle.fleetops.fuel.application.port.out.FuelRecordRepositoryPort;
import co.edu.univalle.fleetops.fuel.application.port.out.OutboxPort;
import co.edu.univalle.fleetops.fuel.application.port.out.RoutePort;
import co.edu.univalle.fleetops.fuel.application.port.out.VehicleAveragePort;
import co.edu.univalle.fleetops.fuel.domain.EfficiencyCalculator;
import co.edu.univalle.fleetops.fuel.domain.FuelAnomalyDetected;
import co.edu.univalle.fleetops.fuel.domain.FuelRecord;
import co.edu.univalle.fleetops.fuel.domain.TankSegment;
import co.edu.univalle.fleetops.fuel.domain.ThresholdAnomalyRule;
import co.edu.univalle.fleetops.fuel.domain.VehicleFuelAverage;
import co.edu.univalle.fleetops.fuel.domain.exception.OdometerNotIncreasingException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterFuelRecordServiceTest {

    @Mock
    private AssignmentPort assignmentPort;
    @Mock
    private RoutePort routePort;
    @Mock
    private FuelRecordRepositoryPort fuelRecordRepository;
    @Mock
    private VehicleAveragePort vehicleAveragePort;
    @Mock
    private OutboxPort outboxPort;

    private RegisterFuelRecordService service;

    @BeforeEach
    void setUp() {
        RegisterFuelRecordPorts ports = new RegisterFuelRecordPorts(assignmentPort, routePort,
                fuelRecordRepository, vehicleAveragePort, outboxPort);
        service = new RegisterFuelRecordService(ports, new EfficiencyCalculator(),
                new ThresholdAnomalyRule(decimal("30"), 2), FIXED_CLOCK, () -> FUEL_RECORD_ID);
    }

    private static RegisterFuelRecordCommand fullTankAt(String odometerKm, String gallons) {
        return new RegisterFuelRecordCommand(ASSIGNMENT_ID, decimal(gallons), decimal("187500.00"),
                decimal(odometerKm), true);
    }

    private void givenAssignmentAndRouteInProgress() {
        when(assignmentPort.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(new AssignmentInfo(ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, true)));
        when(routePort.findRouteInProgress(ASSIGNMENT_ID)).thenReturn(Optional.of(ROUTE_ID));
        when(fuelRecordRepository.findLastOdometerKm(VEHICLE_ID)).thenReturn(Optional.of(decimal("45000.0")));
        when(fuelRecordRepository.save(any(FuelRecord.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void should_store_efficiency_and_update_average_when_consumption_is_normal() {
        // Given: average 20 km/gal, now 250 km with 12.5 gal = 20 km/gal
        givenAssignmentAndRouteInProgress();
        when(fuelRecordRepository.findOpenTankSegment(VEHICLE_ID))
                .thenReturn(Optional.of(new TankSegment(decimal("45000.0"), decimal("0"))));
        when(vehicleAveragePort.findByVehicleId(VEHICLE_ID))
                .thenReturn(Optional.of(new VehicleFuelAverage(VEHICLE_ID, decimal("40.00"), 2)));

        // When
        FuelRecord result = service.register(fullTankAt("45250.0", "12.500"));

        // Then
        assertThat(result.id()).isEqualTo(FUEL_RECORD_ID);
        assertThat(result.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(result.driverId()).isEqualTo(DRIVER_ID);
        assertThat(result.routeId()).isEqualTo(ROUTE_ID);
        assertThat(result.efficiencyKmPerGallon()).isEqualByComparingTo("20.00");
        assertThat(result.anomalousConsumption()).isFalse();
        assertThat(result.recordedAt()).isEqualTo(NOW);
        ArgumentCaptor<VehicleFuelAverage> savedAverage = ArgumentCaptor.forClass(VehicleFuelAverage.class);
        verify(vehicleAveragePort).save(savedAverage.capture());
        assertThat(savedAverage.getValue().sampleCount()).isEqualTo(3);
        verifyNoInteractions(outboxPort);
    }

    @Test
    void should_leave_event_in_outbox_and_not_touch_average_when_consumption_is_anomalous() {
        // Given: average 20 km/gal, now 100 km with 12.5 gal = 8 km/gal (drop of 60 %)
        givenAssignmentAndRouteInProgress();
        when(fuelRecordRepository.findOpenTankSegment(VEHICLE_ID))
                .thenReturn(Optional.of(new TankSegment(decimal("45000.0"), decimal("0"))));
        when(vehicleAveragePort.findByVehicleId(VEHICLE_ID))
                .thenReturn(Optional.of(new VehicleFuelAverage(VEHICLE_ID, decimal("40.00"), 2)));

        // When
        FuelRecord result = service.register(fullTankAt("45100.0", "12.500"));

        // Then
        assertThat(result.anomalousConsumption()).isTrue();
        ArgumentCaptor<FuelAnomalyDetected> event = ArgumentCaptor.forClass(FuelAnomalyDetected.class);
        verify(outboxPort).save(event.capture(), any());
        assertThat(event.getValue().averageEfficiencyKmPerGallon()).isEqualByComparingTo("20.00");
        verify(vehicleAveragePort, never()).save(any());
    }

    @Test
    void should_start_vehicle_average_on_first_efficiency() {
        // Given: the vehicle has a previous full tank but no average yet
        givenAssignmentAndRouteInProgress();
        when(fuelRecordRepository.findOpenTankSegment(VEHICLE_ID))
                .thenReturn(Optional.of(new TankSegment(decimal("45000.0"), decimal("0"))));
        when(vehicleAveragePort.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());

        // When
        FuelRecord result = service.register(fullTankAt("45250.0", "12.500"));

        // Then
        assertThat(result.anomalousConsumption()).isFalse();
        ArgumentCaptor<VehicleFuelAverage> savedAverage = ArgumentCaptor.forClass(VehicleFuelAverage.class);
        verify(vehicleAveragePort).save(savedAverage.capture());
        assertThat(savedAverage.getValue().sampleCount()).isEqualTo(1);
    }

    @Test
    void should_store_without_efficiency_when_tank_is_not_full() {
        // Given
        givenAssignmentAndRouteInProgress();
        RegisterFuelRecordCommand partialLoad = new RegisterFuelRecordCommand(ASSIGNMENT_ID, decimal("5.000"),
                decimal("75000.00"), decimal("45100.0"), false);

        // When
        FuelRecord result = service.register(partialLoad);

        // Then
        assertThat(result.efficiencyKmPerGallon()).isNull();
        assertThat(result.anomalousConsumption()).isFalse();
        verify(fuelRecordRepository, never()).findOpenTankSegment(any());
        verifyNoInteractions(outboxPort);
        verify(vehicleAveragePort, never()).save(any());
    }

    @Test
    void should_store_without_efficiency_when_vehicle_never_filled_the_tank() {
        // Given
        givenAssignmentAndRouteInProgress();
        when(fuelRecordRepository.findOpenTankSegment(VEHICLE_ID)).thenReturn(Optional.empty());

        // When
        FuelRecord result = service.register(fullTankAt("45100.0", "12.500"));

        // Then
        assertThat(result.efficiencyKmPerGallon()).isNull();
        verify(vehicleAveragePort, never()).save(any());
    }

    @Test
    void should_fail_with_not_found_when_assignment_does_not_exist() {
        // Given
        when(assignmentPort.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.register(fullTankAt("45100.0", "12.500")))
                .isInstanceOf(AssignmentNotFoundException.class);
        verifyNoInteractions(routePort, fuelRecordRepository);
    }

    @Test
    void should_fail_with_conflict_when_assignment_is_not_in_progress() {
        // Given
        when(assignmentPort.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(new AssignmentInfo(ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, false)));

        // When / Then
        assertThatThrownBy(() -> service.register(fullTankAt("45100.0", "12.500")))
                .isInstanceOf(AssignmentNotInProgressException.class);
        verifyNoInteractions(routePort, fuelRecordRepository);
    }

    @Test
    void should_fail_with_conflict_when_assignment_has_no_route_in_progress() {
        // Given
        when(assignmentPort.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(new AssignmentInfo(ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, true)));
        when(routePort.findRouteInProgress(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.register(fullTankAt("45100.0", "12.500")))
                .isInstanceOf(RouteNotInProgressException.class);
        verifyNoInteractions(fuelRecordRepository);
    }

    @Test
    void should_reject_odometer_lower_than_last_reading_of_vehicle() {
        // Given
        when(assignmentPort.findById(ASSIGNMENT_ID))
                .thenReturn(Optional.of(new AssignmentInfo(ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, true)));
        when(routePort.findRouteInProgress(ASSIGNMENT_ID)).thenReturn(Optional.of(ROUTE_ID));
        when(fuelRecordRepository.findLastOdometerKm(VEHICLE_ID)).thenReturn(Optional.of(decimal("45000.0")));

        // When / Then
        assertThatThrownBy(() -> service.register(fullTankAt("44999.0", "12.500")))
                .isInstanceOf(OdometerNotIncreasingException.class);
        verify(fuelRecordRepository, never()).save(any());
    }

    @Test
    void should_reject_missing_ports() {
        // Given / When / Then
        assertThatThrownBy(() -> new RegisterFuelRecordPorts(null, routePort, fuelRecordRepository,
                vehicleAveragePort, outboxPort))
                .isInstanceOf(NullPointerException.class);
    }
}
