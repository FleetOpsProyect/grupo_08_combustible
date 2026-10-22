package co.edu.univalle.fleetops.fuel.adapter.out.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

/** Fields of GET /assignment/{id} (Assignments, G2) that Fuel reads. The rest of the response is ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record AssignmentResponse(String state, UUID vehicleId, UUID driverId) {
}
