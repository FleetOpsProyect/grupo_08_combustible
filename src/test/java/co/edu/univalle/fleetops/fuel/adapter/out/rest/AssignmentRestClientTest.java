package co.edu.univalle.fleetops.fuel.adapter.out.rest;

import static co.edu.univalle.fleetops.fuel.FuelTestData.ASSIGNMENT_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.DRIVER_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.VEHICLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.univalle.fleetops.fuel.application.exception.DependencyUnavailableException;
import co.edu.univalle.fleetops.fuel.application.port.out.AssignmentInfo;
import co.edu.univalle.fleetops.fuel.crosscutting.CorrelationIdFilter;
import co.edu.univalle.fleetops.fuel.crosscutting.CorrelationIdPropagationInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Outbound REST adapter of G2: contract translation, 404, failures and open circuit. */
class AssignmentRestClientTest {

    private static final String BASE_URL = "http://assignments.test";

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;
    private AssignmentRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .requestInterceptor(new CorrelationIdPropagationInterceptor());
        server = MockRestServiceServer.bindTo(builder).build();
        circuitBreaker = CircuitBreaker.ofDefaults("assignments-test");
        client = new AssignmentRestClient(builder.build(), circuitBreaker, "IN_PROGRESS");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void should_translate_assignment_and_propagate_correlation_id() {
        // Given
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-g2");
        server.expect(requestTo(BASE_URL + "/assignment/" + ASSIGNMENT_ID))
                .andExpect(header(CorrelationIdFilter.HEADER, "corr-g2"))
                .andRespond(withSuccess("""
                        {"state": "IN_PROGRESS", "vehicle_id": "%s", "driver_id": "%s", "origin": "Cali"}
                        """.formatted(VEHICLE_ID, DRIVER_ID), MediaType.APPLICATION_JSON));

        // When
        Optional<AssignmentInfo> assignment = client.findById(ASSIGNMENT_ID);

        // Then
        assertThat(assignment).contains(new AssignmentInfo(ASSIGNMENT_ID, VEHICLE_ID, DRIVER_ID, true));
        server.verify();
    }

    @Test
    void should_mark_not_in_progress_when_state_is_different() {
        server.expect(requestTo(BASE_URL + "/assignment/" + ASSIGNMENT_ID))
                .andRespond(withSuccess("""
                        {"state": "PENDING", "vehicle_id": "%s", "driver_id": "%s"}
                        """.formatted(VEHICLE_ID, DRIVER_ID), MediaType.APPLICATION_JSON));

        assertThat(client.findById(ASSIGNMENT_ID)).hasValueSatisfying(info -> assertThat(info.inProgress()).isFalse());
    }

    @Test
    void should_return_empty_when_assignments_answers_404() {
        server.expect(requestTo(BASE_URL + "/assignment/" + ASSIGNMENT_ID)).andRespond(withResourceNotFound());

        assertThat(client.findById(ASSIGNMENT_ID)).isEmpty();
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void should_return_empty_when_body_is_empty() {
        server.expect(requestTo(BASE_URL + "/assignment/" + ASSIGNMENT_ID)).andRespond(withSuccess());

        assertThat(client.findById(ASSIGNMENT_ID)).isEmpty();
    }

    @Test
    void should_fail_as_unavailable_when_assignments_answers_500() {
        server.expect(requestTo(BASE_URL + "/assignment/" + ASSIGNMENT_ID)).andRespond(withServerError());

        assertThatThrownBy(() -> client.findById(ASSIGNMENT_ID))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessageContaining("assignments");
    }

    @Test
    void should_fail_fast_without_calling_when_circuit_is_open() {
        // Given
        circuitBreaker.transitionToOpenState();

        // When / Then
        assertThatThrownBy(() -> client.findById(ASSIGNMENT_ID)).isInstanceOf(DependencyUnavailableException.class);
        server.verify();
    }
}
