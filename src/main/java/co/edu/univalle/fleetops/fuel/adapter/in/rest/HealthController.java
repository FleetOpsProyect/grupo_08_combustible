package co.edu.univalle.fleetops.fuel.adapter.in.rest;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health on the API port. Actuator runs on MANAGEMENT_PORT, but the API Gateway and the Docker
 * healthcheck call /health on SERVER_PORT. Delegates to the Actuator HealthEndpoint: 200 when UP, 503 otherwise.
 */
@Hidden
@RestController
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    /**
     * Creates the controller.
     *
     * @param healthEndpoint Actuator health endpoint (lives in the main context even with a separate port)
     */
    public HealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, String>> health() {
        Status status = healthEndpoint.health().getStatus();
        HttpStatus httpStatus = Status.UP.equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(Map.of("status", status.getCode()));
    }
}
