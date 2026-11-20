package co.edu.univalle.fleetops.fuel.adapter.in.rest;

import static co.edu.univalle.fleetops.fuel.FuelTestData.FIXED_CLOCK;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** GET /health on the API port: same answer as the Actuator health endpoint. */
@WebMvcTest(HealthController.class)
@Import(HealthControllerTest.ClockConfig.class)
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthEndpoint healthEndpoint;

    @TestConfiguration
    static class ClockConfig {
        @Bean
        Clock clock() {
            return FIXED_CLOCK;
        }
    }

    @Test
    void should_return_200_when_service_is_up() throws Exception {
        // Given
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        // When / Then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void should_return_503_when_service_is_down() throws Exception {
        // Given
        when(healthEndpoint.health()).thenReturn(Health.down().build());

        // When / Then
        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
