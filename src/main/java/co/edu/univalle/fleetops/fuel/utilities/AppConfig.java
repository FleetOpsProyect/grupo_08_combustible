package co.edu.univalle.fleetops.fuel.utilities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration of the service ("fuel.*" in application.yml). Every value comes from an environment
 * variable documented in .env.example; the service does not start if one is missing or invalid.
 */
@Validated
@ConfigurationProperties(prefix = "fuel")
public record AppConfig(
        @NotBlank String timeZone,
        @NotNull @Valid Anomaly anomaly,
        @NotNull @Valid Outbox outbox,
        @NotNull @Valid Assignments assignments,
        @NotNull @Valid Routes routes,
        @NotNull @Valid Http http,
        @NotNull @Valid CircuitBreakerSettings circuitBreaker) {

    /** Anomaly rule (G08 decision): drop threshold in percent and minimum previous efficiencies. */
    public record Anomaly(@NotNull @Positive @Max(99) BigDecimal thresholdPercent, @Min(1) int minimumSamples) {
    }

    /** Outbox relay: interval between runs and events per run. */
    public record Outbox(@Positive long relayIntervalMs, @Min(1) int batchSize) {
    }

    /** Assignments (G2): base URL and value of "state" for an assignment in progress. */
    public record Assignments(@NotBlank String baseUrl, @NotBlank String inProgressState) {
    }

    /** Telemetry and Routes (G6): base URL and status filter for a route in progress. */
    public record Routes(@NotBlank String baseUrl, @NotBlank String inProgressStatus) {
    }

    /** Timeout of every call to another service (DAS: 2 s). */
    public record Http(@Positive long timeoutMs) {
    }

    /** Circuit Breaker of every outbound REST adapter. */
    public record CircuitBreakerSettings(
            @Positive @Max(100) float failureRateThreshold,
            @Min(1) int slidingWindowSize,
            @Min(1) int minimumCalls,
            @Positive long waitOpenMs) {
    }
}
