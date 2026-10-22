package co.edu.univalle.fleetops.fuel.adapter.out.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

/** Fields of the paginated GET /routes (Telemetry and Routes, G6) that Fuel reads. */
@JsonIgnoreProperties(ignoreUnknown = true)
record RoutePageResponse(List<Route> content) {

    /** One route of the page. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Route(UUID id) {
    }
}
