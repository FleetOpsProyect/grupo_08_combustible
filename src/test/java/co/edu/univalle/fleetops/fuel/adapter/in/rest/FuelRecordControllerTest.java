package co.edu.univalle.fleetops.fuel.adapter.in.rest;

import static co.edu.univalle.fleetops.fuel.FuelTestData.ASSIGNMENT_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.FIXED_CLOCK;
import static co.edu.univalle.fleetops.fuel.FuelTestData.FUEL_RECORD_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.decimal;
import static co.edu.univalle.fleetops.fuel.FuelTestData.fuelRecord;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotFoundException;
import co.edu.univalle.fleetops.fuel.application.exception.AssignmentNotInProgressException;
import co.edu.univalle.fleetops.fuel.application.exception.DependencyUnavailableException;
import co.edu.univalle.fleetops.fuel.application.exception.FuelRecordNotFoundException;
import co.edu.univalle.fleetops.fuel.application.exception.RouteNotInProgressException;
import co.edu.univalle.fleetops.fuel.application.port.in.QueryFuelRecordsUseCase;
import co.edu.univalle.fleetops.fuel.application.port.in.RegisterFuelRecordUseCase;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordPage;
import co.edu.univalle.fleetops.fuel.application.query.FuelRecordSummary;
import co.edu.univalle.fleetops.fuel.crosscutting.CorrelationIdFilter;
import co.edu.univalle.fleetops.fuel.crosscutting.RequestValidator;
import co.edu.univalle.fleetops.fuel.domain.exception.InvalidFuelRecordException;
import co.edu.univalle.fleetops.fuel.domain.exception.OdometerNotIncreasingException;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Inbound REST adapter with MockMvc: HTTP contract, validation and error format. The use cases are mocks. */
@WebMvcTest(FuelRecordController.class)
@Import({RequestValidator.class, FuelRecordControllerTest.ClockConfig.class})
@ActiveProfiles("test")
class FuelRecordControllerTest {

    private static final String VALID_BODY = """
            {"assignment_id": "%s", "gallons_purchased": 12.5, "total_cost": 187500.00,
             "odometer_km": 45250.0, "full_tank": true}
            """.formatted(ASSIGNMENT_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterFuelRecordUseCase registerFuelRecord;

    @MockitoBean
    private QueryFuelRecordsUseCase queryFuelRecords;

    @TestConfiguration
    static class ClockConfig {
        @Bean
        Clock clock() {
            return FIXED_CLOCK;
        }
    }

    @Test
    void should_return_201_with_location_and_snake_case_body_when_record_is_registered() throws Exception {
        // Given
        when(registerFuelRecord.register(any())).thenReturn(fuelRecord(decimal("20.00"), false));

        // When / Then
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(CorrelationIdFilter.HEADER, "corr-123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fuel_records/" + FUEL_RECORD_ID))
                .andExpect(header().string(CorrelationIdFilter.HEADER, "corr-123"))
                .andExpect(jsonPath("$.id").value(FUEL_RECORD_ID.toString()))
                .andExpect(jsonPath("$.efficiency_km_per_gallon").value(20.00))
                .andExpect(jsonPath("$.anomalous_consumption").value(false))
                .andExpect(jsonPath("$.recorded_at").value("2026-10-22T10:00:00-05:00"));
    }

    @Test
    void should_return_400_with_every_invalid_field_when_body_is_invalid() throws Exception {
        // Given
        String invalidBody = """
                {"gallons_purchased": -1, "total_cost": 100, "odometer_km": 10, "full_tank": true}
                """;

        // When / Then
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(CorrelationIdFilter.HEADER))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/fuel_records"))
                .andExpect(jsonPath("$.errors", hasSize(2)));
        verifyNoInteractions(registerFuelRecord);
    }

    @Test
    void should_return_400_when_body_is_not_json() throws Exception {
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void should_return_400_when_domain_rejects_a_value() throws Exception {
        when(registerFuelRecord.register(any())).thenThrow(new InvalidFuelRecordException("total_cost"));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("total_cost"));
    }

    @Test
    void should_return_404_when_assignment_does_not_exist() throws Exception {
        when(registerFuelRecord.register(any())).thenThrow(new AssignmentNotFoundException(ASSIGNMENT_ID));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    void should_return_409_when_assignment_is_not_in_progress() throws Exception {
        when(registerFuelRecord.register(any())).thenThrow(new AssignmentNotInProgressException(ASSIGNMENT_ID));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_NOT_IN_PROGRESS"));
    }

    @Test
    void should_return_409_when_there_is_no_route_in_progress() throws Exception {
        when(registerFuelRecord.register(any())).thenThrow(new RouteNotInProgressException(ASSIGNMENT_ID));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_IN_PROGRESS"));
    }

    @Test
    void should_return_409_when_vehicle_average_changed_at_the_same_time() throws Exception {
        when(registerFuelRecord.register(any())).thenThrow(new OptimisticLockingFailureException("version"));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
    }

    @Test
    void should_return_422_when_odometer_does_not_increase() throws Exception {
        when(registerFuelRecord.register(any()))
                .thenThrow(new OdometerNotIncreasingException(decimal("45300.0"), decimal("45250.0")));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ODOMETER_NOT_INCREASING"));
    }

    @Test
    void should_return_503_when_a_dependency_is_down() throws Exception {
        when(registerFuelRecord.register(any()))
                .thenThrow(new DependencyUnavailableException("assignments", new IllegalStateException("timeout")));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    @Test
    void should_return_500_with_common_format_on_unexpected_error() throws Exception {
        when(registerFuelRecord.register(any())).thenThrow(new IllegalStateException("boom"));
        mockMvc.perform(post("/fuel_records").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void should_return_record_by_id() throws Exception {
        when(queryFuelRecords.findById(FUEL_RECORD_ID)).thenReturn(fuelRecord(decimal("20.00"), false));
        mockMvc.perform(get("/fuel_records/{id}", FUEL_RECORD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment_id").value(ASSIGNMENT_ID.toString()));
    }

    @Test
    void should_return_404_when_record_does_not_exist() throws Exception {
        when(queryFuelRecords.findById(FUEL_RECORD_ID)).thenThrow(new FuelRecordNotFoundException(FUEL_RECORD_ID));
        mockMvc.perform(get("/fuel_records/{id}", FUEL_RECORD_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FUEL_RECORD_NOT_FOUND"));
    }

    @Test
    void should_return_400_when_id_is_not_a_uuid() throws Exception {
        mockMvc.perform(get("/fuel_records/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
    }

    @Test
    void should_return_404_when_path_does_not_exist() throws Exception {
        mockMvc.perform(get("/does_not_exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void should_return_405_when_method_is_not_supported() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/fuel_records/{id}", FUEL_RECORD_ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void should_return_page_with_summary_block() throws Exception {
        // Given
        FuelRecordPage page = new FuelRecordPage(List.of(fuelRecord(decimal("8.00"), true)), 0, 20, 1,
                new FuelRecordSummary(decimal("187500.00"), decimal("12.500"), 1, 1));
        when(queryFuelRecords.search(any(), any())).thenReturn(page);

        // When / Then
        mockMvc.perform(get("/fuel_records").param("anomalous_consumption", "true")
                        .param("date_from", "2026-10-01T00:00:00-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.summary.total_cost").value(187500.00))
                .andExpect(jsonPath("$.summary.anomalous_records").value(1))
                .andExpect(jsonPath("$.summary.total_records").value(1));
    }

    @Test
    void should_return_400_when_page_size_is_above_maximum() throws Exception {
        mockMvc.perform(get("/fuel_records").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].code").value("MAX_VALUE"));
        verifyNoInteractions(queryFuelRecords);
    }
}
