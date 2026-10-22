package co.edu.univalle.fleetops.fuel.adapter.out.rest;

import static co.edu.univalle.fleetops.fuel.FuelTestData.ASSIGNMENT_ID;
import static co.edu.univalle.fleetops.fuel.FuelTestData.ROUTE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.univalle.fleetops.fuel.application.exception.DependencyUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Outbound REST adapter of G6: route in progress of an assignment. */
class RouteRestClientTest {

    private static final String BASE_URL = "http://routes.test";

    private MockRestServiceServer server;
    private RouteRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RouteRestClient(builder.build(), CircuitBreaker.ofDefaults("routes-test"), "IN_PROGRESS");
    }

    @Test
    void should_return_first_route_in_progress() {
        // Given
        server.expect(requestTo(Matchers.startsWith(BASE_URL + "/routes")))
                .andExpect(queryParam("assignment_id", ASSIGNMENT_ID.toString()))
                .andExpect(queryParam("status", "IN_PROGRESS"))
                .andRespond(withSuccess("""
                        {"content": [{"id": "%s", "status": "IN_PROGRESS"}], "page": 0, "size": 20, "total": 1}
                        """.formatted(ROUTE_ID), MediaType.APPLICATION_JSON));

        // When / Then
        assertThat(client.findRouteInProgress(ASSIGNMENT_ID)).contains(ROUTE_ID);
        server.verify();
    }

    @Test
    void should_return_empty_when_there_is_no_route_in_progress() {
        server.expect(requestTo(Matchers.startsWith(BASE_URL + "/routes")))
                .andRespond(withSuccess("{\"content\": [], \"total\": 0}", MediaType.APPLICATION_JSON));

        assertThat(client.findRouteInProgress(ASSIGNMENT_ID)).isEmpty();
    }

    @Test
    void should_return_empty_when_body_is_empty() {
        server.expect(requestTo(Matchers.startsWith(BASE_URL + "/routes"))).andRespond(withSuccess());

        assertThat(client.findRouteInProgress(ASSIGNMENT_ID)).isEmpty();
    }

    @Test
    void should_fail_as_unavailable_when_routes_fails() {
        server.expect(requestTo(Matchers.startsWith(BASE_URL + "/routes"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.findRouteInProgress(ASSIGNMENT_ID))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessageContaining("routes");
    }
}
